Feature: Booking a car
  As a customer I want to book a car for the days I need it,
  and as an agency I want to approve, reject or cancel requests,
  so that every car is rented out without ever being double-booked.

  Background:
    Given agency 1 owns car 10 priced at 100 per day
    And customer 2 is logged in

  Scenario: A customer books a car for three days
    When customer 2 books car 10 from 5 to 8 days from now
    Then the booking is "PENDING" with a total price of 300
    And a "BOOKING_CREATED" event is published

  Scenario: The return date must be after the pick-up date
    When customer 2 books car 10 from 8 to 5 days from now
    Then the request is rejected with "Start date must be before end date"

  Scenario: A booking cannot start in the past
    When customer 2 books car 10 from -2 to 3 days from now
    Then the request is rejected with "Booking cannot start in the past"

  Scenario: An unavailable car cannot be booked
    Given car 10 is marked unavailable
    When customer 2 books car 10 from 5 to 8 days from now
    Then the request is rejected with "Car not found"

  Scenario: Dates that overlap an approved booking are refused
    Given car 10 has an approved booking from 4 to 9 days from now
    When customer 2 books car 10 from 6 to 10 days from now
    Then the request is rejected with "Car is not available for that specific date"

  Scenario: The car owner approves a pending booking
    Given customer 2 has a pending booking 100 for car 10 from 5 to 8 days from now
    When agency 1 approves booking 100
    Then booking 100 is "APPROVED"
    And a "BOOKING_APPROVED" event is published

  Scenario: Only the car owner can approve a booking
    Given customer 2 has a pending booking 100 for car 10 from 5 to 8 days from now
    When agency 3 approves booking 100
    Then the request is rejected with "You do not own this car"
    And booking 100 is "PENDING"

  Scenario: The car owner rejects a pending booking
    Given customer 2 has a pending booking 100 for car 10 from 5 to 8 days from now
    When agency 1 rejects booking 100
    Then booking 100 is "REJECTED"
    And a "BOOKING_REJECTED" event is published

  Scenario: Cancelling a paid booking refunds the customer
    Given customer 2 has a paid approved booking 100 for car 10 from 5 to 8 days from now
    When customer 2 cancels booking 100
    Then booking 100 is "CANCELLED"
    And booking 100 is refunded through the payment service
    And a "BOOKING_CANCELLED" event is published

  Scenario: Two overlapping approvals at the same moment
    Given customer 2 has a pending booking 100 for car 10 from 5 to 8 days from now
    And customer 2 has a pending booking 101 for car 10 from 6 to 9 days from now
    When agency 1 approves bookings 100 and 101 at the same time
    Then exactly one of the two bookings is "APPROVED"
