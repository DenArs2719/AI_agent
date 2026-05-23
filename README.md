# Sears Home Services Voice AI Backend

Backend implementation for a Sears Home Services voice diagnostic agent. The app accepts inbound Twilio voice webhooks, collects appliance diagnostic information with OpenAI, guides the caller through safe troubleshooting checks, and schedules a technician when service is needed.

See [TECHNICAL_DESIGN.md](TECHNICAL_DESIGN.md) for architectural decisions, tradeoffs, and technology rationale.

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA / Hibernate
- PostgreSQL 16
- Docker Compose
- Twilio Voice webhooks and TwiML
- OpenAI Responses API for dialogue extraction
- Maven

## Architecture

```text
Caller
  -> Twilio phone number
  -> POST /voice/incoming
  -> VoiceWebhookController
  -> VoiceWebhookService
      -> CallSessionRepository
      -> OpenAiDiagnosticService
      -> TroubleshootingScriptService
      -> SchedulingService
      -> AppointmentService
  -> TwiML response back to Twilio
  -> Caller hears Twilio text-to-speech
```

The voice flow is intentionally split between AI extraction and deterministic backend logic:

- OpenAI extracts structured updates from caller speech.
- The backend decides the next required conversation stage.
- The backend performs technician matching and appointment creation.
- The backend never lets the model choose technicians, book appointments, or skip required fields.
- If OpenAI or another dialogue step fails, the session is marked `FAILED`, error details are stored, and Twilio receives a retry prompt instead of an HTTP 500.

## Main Flows

### Voice Flow

1. `POST /voice/incoming`
   - Creates or loads a `CallSession` by Twilio `CallSid`.
   - Stores caller phone number when provided.
   - Returns TwiML with a speech `<Gather>`.

2. `POST /voice/respond`
   - Receives Twilio `SpeechResult`.
   - Sends current session state and caller speech to OpenAI.
   - Applies only newly captured fields.
   - Chooses the next missing stage in Java.
   - Returns TwiML with the next prompt.

3. When all required fields are present:
   - Finds eligible technicians and open slots.
   - Proposes the first matching slot.
   - Waits for verbal confirmation.
   - Creates and confirms the appointment.

Each Twilio call has its own `CallSid`, and the application uses that value as the unique key for a `CallSession`. If the same customer calls again later, Twilio sends a new `CallSid`, so the app creates a new `CallSession` even when the caller phone number is the same. The previous appointment remains stored in `appointments`; future follow-up or rescheduling logic could use `caller_phone_number` to find an existing customer or recent appointment.

### Conversation Stages

The conversation is tracked in the `call_sessions` table:

```text
APPLIANCE_TYPE
SYMPTOMS
ERROR_CODES
TROUBLESHOOTING_STEPS
ZIP_CODE
CUSTOMER_NAME
AVAILABILITY
READY_TO_SCHEDULE
SLOT_CONFIRMATION
APPOINTMENT_CONFIRMED
FAILED
ABANDONED
```

The required diagnostic fields are:

- appliance type
- symptoms
- error codes
- troubleshooting steps
- ZIP code
- customer name
- availability

Failure/debug fields are also stored:

- `stage_before_failure`
- `last_error_message`
- `last_error_at`
- `error_count`

## Database Model

Core tables:

- `technicians`
- `technician_service_areas`
- `technician_specialties`
- `availability_slots`
- `customers`
- `appointments`
- `call_sessions`

`DataSeeder` creates representative sample data:

- 8 technicians
- multiple Chicago ZIP codes
- multiple appliance specialties
- open availability slots
- sample customers and appointments

## Scheduling Logic

`SchedulingService` matches technicians by:

1. Customer ZIP code
2. Appliance type
3. Unbooked availability slot inside the requested time window

Filtering is pushed into PostgreSQL through `AvailabilitySlotRepository.findMatchingOpenSlots(...)`. Java then groups returned slots by technician and builds the API response.

Indexes support the main scheduling query:

- `technician_service_areas(zip_code)`
- `technician_service_areas(technician_id)`
- `technician_specialties(specialty)`
- `technician_specialties(technician_id)`
- `availability_slots(booked, starts_at, ends_at)`
- `availability_slots(technician_id)`

Availability phrases are mapped to time windows:

```text
morning   -> 08:00-12:00
afternoon -> 12:00-17:00
evening   -> 17:00-21:00
unknown   -> 08:00-18:00
```

When the caller accepts the proposed slot, `AppointmentService` creates the appointment, marks the slot booked, and confirms the appointment.

## Troubleshooting Guidance

`TroubleshootingScriptService` contains safe basic checks for:

- washer
- dryer
- refrigerator
- dishwasher
- oven
- HVAC

The voice flow reads appliance-specific checks during the troubleshooting stage, for example power, door/lid closed, breaker, filters, vents, water supply, and visible error codes. It avoids unsafe instructions such as internal wiring, panels, or gas-line inspection.

## API Endpoints

### Voice Webhooks

```text
POST /voice/incoming
POST /voice/respond
```

Twilio parameters used:

- `CallSid`
- `From`
- `SpeechResult`

### Scheduling

```text
GET /api/scheduling/matches
```

Query parameters:

- `customerZipCode`
- `applianceType`
- `desiredStart`
- `desiredEnd`

Example:

```powershell
Invoke-RestMethod "http://localhost:8080/api/scheduling/matches?customerZipCode=60601&applianceType=REFRIGERATOR&desiredStart=2026-05-24T08:00:00&desiredEnd=2026-05-24T12:00:00"
```

### Appointments

```text
POST /api/appointments
POST /api/appointments/{appointmentId}/confirm
```

Example appointment request:

```json
{
  "availabilitySlotId": 1,
  "applianceType": "REFRIGERATOR",
  "issueDescription": "Refrigerator is leaking. No error code. Customer checked power and doors.",
  "customerFirstName": "Denise",
  "customerLastName": "Smith",
  "customerPhoneNumber": "+48609730999",
  "customerEmail": "denise@example.com",
  "customerStreetAddress": "120 N State St",
  "customerZipCode": "60601"
}
```

### Troubleshooting Scripts

```text
GET /api/troubleshooting
GET /api/troubleshooting/{applianceType}
```

Example:

```powershell
Invoke-RestMethod "http://localhost:8080/api/troubleshooting/REFRIGERATOR"
```

## Environment Variables

Create a `.env` file in the project root:

```env
POSTGRES_DB=shs_voice_ai
POSTGRES_USER=shs_user
POSTGRES_PASSWORD=shs_password

SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/shs_voice_ai
SPRING_DATASOURCE_USERNAME=shs_user
SPRING_DATASOURCE_PASSWORD=shs_password

TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=+19129551708

AI_API_KEY=your_openai_api_key
AI_MODEL=gpt-4o-mini

MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=mail_username
MAIL_PASSWORD=mail_password
MAIL_FROM=no-reply@example.com
```

Mail settings are included for future visual-diagnosis expansion, but Tier 1 and Tier 2 do not require email sending.

## Running Locally With Docker

```powershell
cd C:\Workspace\demo
docker compose up --build
```

This starts:

- PostgreSQL on port `5432`
- Spring Boot app on port `8080`

To rebuild cleanly:

```powershell
docker compose down
docker compose build --no-cache app
docker compose up
```

If enum stages or schema constraints changed during development, reset the demo database:

```powershell
docker compose down -v
docker compose up --build
```

## Testing Voice Flow Without a Real Phone Call

Start a session:

```powershell
$response = Invoke-WebRequest "http://localhost:8080/voice/incoming" `
  -UseBasicParsing `
  -Method Post `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "CallSid=CA_CONSOLE_TEST_1&From=%2B48609730999"

$response.Content
```

Send caller speech:

```powershell
$response = Invoke-WebRequest "http://localhost:8080/voice/respond" `
  -UseBasicParsing `
  -Method Post `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "CallSid=CA_CONSOLE_TEST_1&From=%2B48609730999&SpeechResult=My refrigerator is leaking in 60601"

$response.Content
```

Continue with the same `CallSid` for the whole conversation. Use a new `CallSid` for a clean new test.

## Twilio And Ngrok Setup

Expose the local app:

```powershell
ngrok http 8080
```

Configure the Twilio number:

```text
Phone Numbers -> Manage -> Active numbers -> selected number -> Voice Configuration
```

Set:

```text
A call comes in: Webhook
Method: HTTP POST
URL: https://your-ngrok-domain.ngrok-free.dev/voice/incoming
```

For trial accounts, Twilio may block calls from unverified caller IDs. If a call log shows warning `21264 From number not verified`, Twilio rejected the call before it reached this app. In that case, test with simulated webhook requests or use an upgraded/verified Twilio account.

## Useful Database Checks

Connect to PostgreSQL:

```powershell
docker exec -it shs-postgres psql -U shs_user -d shs_voice_ai
```

View latest call sessions:

```sql
select
  id,
  call_sid,
  current_stage,
  stage_before_failure,
  appliance_type,
  symptoms,
  error_codes,
  prior_troubleshooting_steps,
  zip_code,
  customer_name,
  availability,
  proposed_technician_name,
  proposed_slot_id,
  appointment_id,
  error_count,
  last_error_at,
  last_error_message
from call_sessions
order by id desc;
```

Check indexes:

```sql
select indexname, indexdef
from pg_indexes
where tablename in (
  'call_sessions',
  'technician_service_areas',
  'technician_specialties',
  'availability_slots'
)
order by tablename, indexname;
```

## Tests

Run all tests:

```powershell
.\mvnw.cmd test
```

Build the app:

```powershell
.\mvnw.cmd -DskipTests package
```

## Design Tradeoffs

- Twilio is used for telephony, speech gathering, and text-to-speech through TwiML. This keeps the demo small and avoids separate STT/TTS services.
- OpenAI is used for structured dialogue extraction, not for scheduling decisions.
- Scheduling and appointment creation are deterministic Java logic for reliability and testability.
- PostgreSQL stores both scheduling data and call-session memory.
- Scheduling filters run in the database for better scalability than loading all technicians into memory.
- The current implementation is a monolith. In a microservice version, the voice service would call scheduling and appointment services through API client classes instead of direct service calls.
- Visual diagnosis and image upload are intentionally out of scope for Tier 1 and Tier 2.
