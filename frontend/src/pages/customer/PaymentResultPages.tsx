import { Link, useSearchParams } from 'react-router-dom';

// Stripe redirects here after its hosted Checkout page (see StripeService.checkout and app.frontend.url).
// The booking is marked paid by the Stripe webhook, not by this page, so the page never claims more than
// Stripe told the browser: the "Paid" badge appears in My bookings once the webhook has been processed.

/** /payment/success?session_id=... */
export function PaymentSuccessPage() {
  return (
    <div className="card payment-result">
      <div className="payment-result-icon success" aria-hidden>
        ✓
      </div>
      <h1>Payment received</h1>
      <p className="muted">
        Thanks! Stripe has confirmed your payment. Your booking will show as <strong>Paid</strong> in a few
        seconds.
      </p>
      <div className="form-actions center-actions">
        <Link to="/bookings" className="btn btn-primary">
          Go to my bookings
        </Link>
        <Link to="/cars" className="btn btn-ghost">
          Browse more cars
        </Link>
      </div>
    </div>
  );
}

/** /payment/cancel?bookingId=... */
export function PaymentCancelPage() {
  const [params] = useSearchParams();
  const bookingId = params.get('bookingId');

  return (
    <div className="card payment-result">
      <div className="payment-result-icon cancel" aria-hidden>
        ✕
      </div>
      <h1>Payment cancelled</h1>
      <p className="muted">
        You left the payment page, so nothing was charged.
        {bookingId && <> Booking #{bookingId} is still approved and waiting for payment.</>}
      </p>
      <div className="form-actions center-actions">
        <Link to="/bookings" className="btn btn-primary">
          Back to my bookings
        </Link>
      </div>
    </div>
  );
}
