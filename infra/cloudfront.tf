locals {
  api_routes = {
    "/api/users*"         = 8081
    "/api/profiles*"      = 8081
    "/api/cars*"          = 8082
    "/api/categories*"    = 8082
    "/api/makes*"         = 8082
    "/api/countries*"     = 8082
    "/api/cities*"        = 8082
    "/api/images*"        = 8082
    "/api/bookings*"      = 8083
    "/api/pay*"           = 8084
    "/api/notifications*" = 8085
    "/api/audit-logs*"    = 8086
  }
  api_ports = toset([for port in values(local.api_routes) : tostring(port)])
}

resource "aws_cloudfront_function" "strip_api" {
  name    = "${var.project_name}-strip-api"
  runtime = "cloudfront-js-2.0"
  publish = true
  code    = <<-EOT
    function handler(event) {
      var request = event.request;
      request.uri = request.uri.replace(/^\/api/, '') || '/';
      return request;
    }
  EOT
}

resource "aws_cloudfront_distribution" "main" {
  enabled             = true
  default_root_object = "index.html"
  price_class         = "PriceClass_100"

  origin {
    origin_id   = "frontend"
    domain_name = aws_s3_bucket_website_configuration.frontend.website_endpoint

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  dynamic "origin" {
    for_each = local.api_ports
    content {
      origin_id   = "api-${origin.value}"
      domain_name = aws_instance.app.public_dns

      custom_origin_config {
        http_port              = tonumber(origin.value)
        https_port             = 443
        origin_protocol_policy = "http-only"
        origin_ssl_protocols   = ["TLSv1.2"]
        origin_read_timeout    = 60
      }
    }
  }

  default_cache_behavior {
    target_origin_id       = "frontend"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    cache_policy_id        = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  }

  dynamic "ordered_cache_behavior" {
    for_each = local.api_routes
    content {
      path_pattern           = ordered_cache_behavior.key
      target_origin_id       = "api-${ordered_cache_behavior.value}"
      viewer_protocol_policy = "redirect-to-https"
      allowed_methods        = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
      cached_methods         = ["GET", "HEAD"]

      forwarded_values {
        query_string = true
        headers      = ["*"]
        cookies {
          forward = "all"
        }
      }
      min_ttl     = 0
      default_ttl = 0
      max_ttl     = 0

      function_association {
        event_type   = "viewer-request"
        function_arn = aws_cloudfront_function.strip_api.arn
      }
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }
}