/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import com.fasterxml.jackson.annotation.*;
import org.apache.avro.*;
import org.apache.avro.generic.*;
import io.confluent.kafka.schemaregistry.avro.*;
import java.math.*;
import java.util.*;

import kickstarter.helper.*;


public class AirlineData {
    // --- The field names of the class object
    public static final String FIELD_EMAIL_ADDRESS = "email_address";
    public static final String FIELD_DEPARTURE_TIME = "departure_time";
    public static final String FIELD_DEPARTURE_AIRPORT_CODE = "departure_airport_code";
	public static final String FIELD_ARRIVAL_TIME = "arrival_time";
    public static final String FIELD_ARRIVAL_AIRPORT_CODE = "arrival_airport_code";
    public static final String FIELD_FLIGHT_DURATION = "flight_duration";
    public static final String FIELD_FLIGHT_NUMBER = "flight_number";
    public static final String FIELD_CONFIRMATION_CODE = "confirmation_code";
    public static final String FIELD_TICKET_PRICE = "ticket_price";
    public static final String FIELD_AIRCRAFT = "aircraft";
    public static final String FIELD_BOOKING_AGENCY_EMAIL = "booking_agency_email";

    // --- The subject name of the class object
    public static final String SUBJECT_AIRLINE_DATA = "AirlineData";
    public static final String NAMESPACE_THEJ3_MODEL = "com.thej3.apache_flink_kickstater.model";

    @JsonProperty(FIELD_EMAIL_ADDRESS)
    private String email_address;

    @JsonProperty(FIELD_DEPARTURE_TIME)
    private String departure_time;

    @JsonProperty(FIELD_DEPARTURE_AIRPORT_CODE)
    private String departure_airport_code;

    @JsonProperty(FIELD_ARRIVAL_TIME)
    private String arrival_time;

    @JsonProperty(FIELD_ARRIVAL_AIRPORT_CODE)
    private String arrival_airport_code;

    @JsonProperty(FIELD_FLIGHT_DURATION)
    private long flight_duration;

    @JsonProperty(FIELD_FLIGHT_NUMBER)
    private String flight_number;

    @JsonProperty(FIELD_CONFIRMATION_CODE)
    private String confirmation_code;

    @JsonProperty(FIELD_TICKET_PRICE)
    private BigDecimal ticket_price;

    @JsonProperty(FIELD_AIRCRAFT)
    private String aircraft;

    @JsonProperty(FIELD_BOOKING_AGENCY_EMAIL)
    private String booking_agency_email;


    /**
     * Default constructor.
     */
	public AirlineData() {}


    public String getEmailAddress() {
        return this.email_address;
    }

    public void setEmailAddress(String emailAddress) {
        this.email_address = emailAddress;
    }

    public String getDepartureTime() {
        return this.departure_time;
    }

    public void setDepartureTime(String departureTime) {
        this.departure_time = departureTime;
    }

    public String getDepartureAirportCode() {
        return this.departure_airport_code;
    }

    public void setDepartureAirportCode(String departureAirport) {
        this.departure_airport_code = departureAirport;
    }

    public String getArrivalTime() {
        return this.arrival_time;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrival_time = arrivalTime;
    }

    public String getArrivalAirportCode() {
        return this.arrival_airport_code;
    }

    public void setArrivalAirportCode(String arrivalAirport) {
        this.arrival_airport_code = arrivalAirport;
    }

    public long getFlightDuration() {
        return this.flight_duration;
    }

    public void setFlightDuration(long flightDuration) {
        this.flight_duration = flightDuration;
    }

    public String getFlightNumber() {
        return this.flight_number;
    }

    public void setFlightNumber(String flightNumber) {
        this.flight_number = flightNumber;
    }

    public String getConfirmationCode() {
        return this.confirmation_code;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmation_code = confirmationCode;
    }

    public BigDecimal getTicketPrice() {
        return this.ticket_price;
    }

    public void setTicketPrice(BigDecimal totalPrice) {
        this.ticket_price = totalPrice;
    }

    public String getAircraft() {
        return this.aircraft;
    }

    public void setAircraft(String aircraft) {
        this.aircraft = aircraft;
    }

    public String getBookingAgencyEmail() {
        return this.booking_agency_email;
    }

    public void setBookingAgencyEmail(String bookingAgencyEmail) {
        this.booking_agency_email = bookingAgencyEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AirlineData that = (AirlineData) o;
        return Objects.equals(getEmailAddress(), that.email_address) && 
                                Objects.equals(getDepartureTime(), that.departure_time) && 
                                Objects.equals(getDepartureAirportCode(), that.departure_airport_code) && 
                                Objects.equals(getArrivalTime(), that.arrival_time) && 
                                Objects.equals(getArrivalAirportCode(), that.arrival_airport_code) && 
                                Objects.equals(getFlightDuration(), that.flight_duration) && 
                                Objects.equals(getFlightNumber(), that.flight_number) && 
                                Objects.equals(getConfirmationCode(), that.confirmation_code) && 
                                Objects.equals(getTicketPrice(), that.ticket_price) && 
                                Objects.equals(getAircraft(), that.aircraft) &&
                                Objects.equals(getBookingAgencyEmail(), that.booking_agency_email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEmailAddress(),
                            getDepartureTime(), 
                            getDepartureAirportCode(), 
                            getArrivalTime(), 
                            getArrivalAirportCode(),
                            getFlightDuration(), 
                            getFlightNumber(), 
                            getConfirmationCode(), 
                            getTicketPrice(), 
                            getAircraft(),
                            getBookingAgencyEmail());
    }

    @Override
    public String toString() {
        return "AirlineData{" +
            "email_address='" + getEmailAddress() + '\'' +
            ", departure_time=" + getDepartureTime() +
            ", departure_airport_code='" + getDepartureAirportCode() + '\'' +
            ", arrival_time=" + getArrivalTime() +
            ", arrival_airport_code='" + getArrivalAirportCode() + '\'' +
            ", flight_duration=" + getFlightDuration() +
            ", flight_number='" + getFlightNumber() + '\'' +
            ", confirmation_code='" + getConfirmationCode() + '\'' +
            ", ticket_price=" + getTicketPrice() +
            ", aircraft='" + getAircraft() + '\'' +
            ", booking_agency_email='" + getBookingAgencyEmail() + '\'' +
            '}';
    }
    public FlightData toFlightData(final String airline) {
        FlightData flightData = new FlightData();

        flightData.setEmailAddress(getEmailAddress());
        flightData.setDepartureTime(getDepartureTime());
        flightData.setDepartureAirportCode(getDepartureAirportCode());
        flightData.setArrivalTime(getArrivalTime());
        flightData.setArrivalAirportCode(getArrivalAirportCode());
        flightData.setFlightNumber(getFlightNumber());
        flightData.setConfirmationCode(getConfirmationCode());
        flightData.setAirline(airline);

        return flightData;
    }

    /**
     * The method converts the instantiated class object into a generic record.
     * 
     * @return the generic record of the instantiated class object.
     * @throws AvroSchemaFieldNotExistException - The exception is thrown when the
     * schema field does not exist.
     */
    public GenericRecord toGenericRecord() throws AvroSchemaFieldNotExistException {
        Schema schema = buildSchema().rawSchema();
        GenericRecord genericRecord = new GenericData.Record(schema);
        AvroHelper.setRecordField(genericRecord, schema, FIELD_EMAIL_ADDRESS, getEmailAddress());
        AvroHelper.setRecordField(genericRecord, schema, FIELD_DEPARTURE_TIME, getDepartureTime());
        AvroHelper.setRecordField(genericRecord, schema, FIELD_DEPARTURE_AIRPORT_CODE, getDepartureAirportCode());
        AvroHelper.setRecordField(genericRecord, schema, FIELD_ARRIVAL_TIME, getArrivalTime());
        AvroHelper.setRecordField(genericRecord, schema, FIELD_ARRIVAL_AIRPORT_CODE, getArrivalAirportCode());
        AvroHelper.setRecordField(genericRecord, schema, FIELD_FLIGHT_DURATION, getFlightDuration());
        AvroHelper.setRecordField(genericRecord, schema, FIELD_FLIGHT_NUMBER, getFlightNumber());
        AvroHelper.setRecordField(genericRecord, schema, FIELD_CONFIRMATION_CODE, getConfirmationCode());
        AvroHelper.setRecordField(genericRecord, schema, FIELD_TICKET_PRICE, getTicketPrice());
        AvroHelper.setRecordField(genericRecord, schema, FIELD_AIRCRAFT, getAircraft());
        AvroHelper.setRecordField(genericRecord, schema, FIELD_BOOKING_AGENCY_EMAIL, getBookingAgencyEmail());
        return genericRecord;
    }

    /**
     * The method creates the class object record schema.
     * 
     * @return the class objects converts the underlying org.apache.avro.Schema 
     * to an io.confluent.kafka.schemaregistry.avro.AvroSchema because it is tailored 
     * to the needs of schema management within the ecosystem of Kafka and the 
     * Confluent Schema Registry.  Moreover, the capabilities of 
     * io.confluent.kafka.schemaregistry.avro.AvroSchema extends the ability to support
     * the Confluent Schema Registry for registering, retrieving, and managing schemas.
     * These include compatibility checks, schema versioning, and other registry-specific
     * operations.
     */
    public AvroSchema buildSchema() {
        // --- Returns the defined schema
        return 
            new AvroSchema(SchemaBuilder
                .record(SUBJECT_AIRLINE_DATA).namespace(NAMESPACE_THEJ3_MODEL)
                    .doc("")
                    .fields()
                        .name(FIELD_EMAIL_ADDRESS)
                            .doc("")
                            .type().stringType().noDefault()
                        .name(FIELD_DEPARTURE_TIME)
                            .doc("")
                            .type().stringType().noDefault()
                        .name(FIELD_DEPARTURE_AIRPORT_CODE)
                            .doc("")
                            .type().stringType().noDefault()
                        .name(FIELD_ARRIVAL_TIME)
                            .doc("")
                            .type().intType().noDefault()
                        .name(FIELD_ARRIVAL_AIRPORT_CODE)
                            .doc("")
                            .type().stringType().noDefault()
                        .name(FIELD_FLIGHT_DURATION)
                            .doc("")
                            .type().longType().noDefault()
                        .name(FIELD_FLIGHT_NUMBER)
                            .doc("")
                            .type().stringType().noDefault()
                        .name(FIELD_CONFIRMATION_CODE)
                            .doc("")
                            .type().stringType().noDefault()
                        .name(FIELD_TICKET_PRICE)
                            .doc("")
                            /*
                             * Since Avro does not have a built-in decimal type, the logical type of decimal
                             * is used to represent the decimal type. The logical type of decimal is represented
                             * by bytes.
                             */
                            .type(LogicalTypes.decimal(10, 2).addToSchema(Schema.create(Schema.Type.BYTES))).noDefault()
                        .name(FIELD_AIRCRAFT)
                            .doc("")
                            .type().stringType().noDefault()
                        .name(FIELD_BOOKING_AGENCY_EMAIL)
                            .doc("")
                            .type().stringType().noDefault()
                .endRecord());
    }
}
