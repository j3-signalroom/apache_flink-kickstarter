/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class is used to represent the Avro schema for the AirlineAvroData record.
 */
package kickstarter.model;

import org.apache.avro.specific.*;
import org.apache.avro.*;
import java.math.*;
import java.util.*;
import java.nio.ByteBuffer;
import org.apache.avro.Conversions.DecimalConversion;
import org.apache.avro.reflect.AvroName;


public class AirlineAvroData extends SpecificRecordBase implements SpecificRecord  {
    public static final Schema SCHEMA = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"AirlineAvroData\",\"namespace\":\"kickstarter.model\",\"fields\":[{\"name\":\"email_address\",\"type\":\"string\"},{\"name\":\"departure_time\",\"type\":\"string\"},{\"name\":\"departure_airport_code\",\"type\":\"string\"},{\"name\":\"arrival_time\",\"type\":\"string\"},{\"name\":\"arrival_airport_code\",\"type\":\"string\"},{\"name\":\"flight_duration\",\"type\":\"long\"},{\"name\":\"flight_number\",\"type\":\"string\"},{\"name\":\"confirmation_code\",\"type\":\"string\"},{\"name\":\"ticket_price\",\"type\":{\"type\":\"bytes\",\"logicalType\":\"decimal\",\"precision\":10,\"scale\":2}},{\"name\":\"aircraft\",\"type\":\"string\"},{\"name\":\"booking_agency_email\",\"type\":\"string\"}]}");
    public static final String SUBJECT = "kickstarter.model.AirlineAvroData";

    @AvroName("email_address")
    private String emailAddress;
    
    @AvroName("departure_time")
    private String departureTime;

    @AvroName("departure_airport_code")
    private String departureAirportCode;

    @AvroName("arrival_time")
    private String arrivalTime;

    @AvroName("arrival_airport_code")
    private String arrivalAirportCode;

    @AvroName("flight_duration")
    private long flightDuration;

    @AvroName("flight_number")
    private String flightNumber;

    @AvroName("confirmation_code")
    private String confirmationCode;

    @AvroName("ticket_price")
    private BigDecimal ticketPrice;

    @AvroName("aircraft")
    private String aircraft;

    @AvroName("booking_agency_email")
    private String bookingAgencyEmail;

    /*
     * Since the class implements Serializable via SpecificRecordBase, a serialVersionUID
     * is added.  A SerialVersionUID identifies the unique original class version for which
     * this class is capable of writing streams and from which it can read.
     */
    private static final long serialVersionUID = 1L;

    private static final DecimalConversion DECIMAL_CONVERSION = new DecimalConversion();
    private static final LogicalTypes.Decimal DECIMAL_TYPE = LogicalTypes.decimal(10, 2);
    private static final Schema DECIMAL_SCHEMA = DECIMAL_TYPE.addToSchema(Schema.create(Schema.Type.BYTES));


    /**
     * Default constructor.
     */
	public AirlineAvroData() {}


    public String getEmailAddress() {
        return this.emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getDepartureTime() {
        return this.departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getDepartureAirportCode() {
        return this.departureAirportCode;
    }

    public void setDepartureAirportCode(String departureAirportCode) {
        this.departureAirportCode = departureAirportCode;
    }

    public String getArrivalTime() {
        return this.arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getArrivalAirportCode() {
        return this.arrivalAirportCode;
    }

    public void setArrivalAirportCode(String arrivalAirportCode) {
        this.arrivalAirportCode = arrivalAirportCode;
    }

    public long getFlightDuration() {
        return this.flightDuration;
    }

    public void setFlightDuration(long flightDuration) {
        this.flightDuration = flightDuration;
    }

    public String getFlightNumber() {
        return this.flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getConfirmationCode() {
        return this.confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public BigDecimal getTicketPrice() {
        return this.ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public String getAircraft() {
        return this.aircraft;
    }

    public void setAircraft(String aircraft) {
        this.aircraft = aircraft;
    }

    public String getBookingAgencyEmail() {
        return this.bookingAgencyEmail;
    }

    public void setBookingAgencyEmail(String bookingAgencyEmail) {
        this.bookingAgencyEmail = bookingAgencyEmail;
    }


    /**
     * This method is used to compare two objects of the same type.
     * 
     * @param o The object to compare.
     * 
     * @return boolean True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) 
            return true;
        if (o == null || getClass() != o.getClass()) 
            return false;
        AirlineAvroData that = (AirlineAvroData) o;

        return Objects.equals(getEmailAddress(), that.getEmailAddress()) && 
                Objects.equals(getDepartureTime(), that.getDepartureTime()) && 
                Objects.equals(getDepartureAirportCode(), that.getDepartureAirportCode()) && 
                Objects.equals(getArrivalTime(), that.getArrivalTime()) && 
                Objects.equals(getArrivalAirportCode(), that.getArrivalAirportCode()) && 
                getFlightDuration() == that.getFlightDuration() && 
                Objects.equals(getFlightNumber(), that.getFlightNumber()) && 
                Objects.equals(getConfirmationCode(), that.getConfirmationCode()) && 
                Objects.equals(getTicketPrice(), that.getTicketPrice()) && 
                Objects.equals(getAircraft(), that.getAircraft()) &&
                Objects.equals(getBookingAgencyEmail(), that.getBookingAgencyEmail());
    }

    /**
     * This method is used to generate a hash code for the object.
     * 
     * @return int The hash code for the object.
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(getEmailAddress(),
                                    getDepartureTime(), 
                                    getDepartureAirportCode(), 
                                    getArrivalTime(), 
                                    getArrivalAirportCode(),
                                    getFlightNumber(), 
                                    getConfirmationCode(), 
                                    getTicketPrice(), 
                                    getAircraft(),
                                    getBookingAgencyEmail());
        result = 31 * result + Long.hashCode(getFlightDuration());
        return result;
    }
    
    /**
     * @return a string representation of the object.
     * Useful for debugging and logging.
     */
    @Override
    public String toString() {
        return "AirlineAvroData{" +
            "emailAddress='" + getEmailAddress() + "'" +
            ", departureTime=" + getDepartureTime() +
            ", departureAirportCode='" + getDepartureAirportCode() + "'" +
            ", arrivalTime=" + getArrivalTime() +
            ", arrivalAirportCode='" + getArrivalAirportCode() + "'" +
            ", flightDuration=" + getFlightDuration() +
            ", flightNumber='" + getFlightNumber() + "'" +
            ", confirmationCode='" + getConfirmationCode() + "'" +
            ", ticketPrice=" + getTicketPrice() +
            ", aircraft='" + getAircraft() + "'" +
            ", bookingAgencyEmail='" + getBookingAgencyEmail() + "'" +
            '}';
    }

    /**
     * This method is used by the serializer to get the schema.
     * 
     * @return The schema of the record.
     */
    @Override
    public Schema getSchema() {
        return SCHEMA;
    }

    /**
     * This method is used by the serializer to get the values of the fields.
     * 
     * @param fieldIndex The index of the field to get.
     * 
     * @return The value of the field.
     */
    @Override
    public Object get(int fieldIndex) {
        switch (fieldIndex) {
            case 0: 
                return getEmailAddress();
            case 1: 
                return getDepartureTime();
            case 2: 
                return getDepartureAirportCode();
            case 3: 
                return getArrivalTime();
            case 4: 
                return getArrivalAirportCode();
            case 5: 
                return getFlightDuration();
            case 6: 
                return getFlightNumber();
            case 7: 
                return getConfirmationCode();
            case 8:
                return DECIMAL_CONVERSION.toBytes(getTicketPrice(), DECIMAL_SCHEMA, DECIMAL_TYPE);
            case 9: 
                return getAircraft();
            case 10: 
                return getBookingAgencyEmail();
            default: 
                throw new IndexOutOfBoundsException("Invalid field index: " + fieldIndex);
        }
    }

    /**
     * This method is used by the deserializer to set the values of the fields.
     * 
     * @param fieldIndex The index of the field to set.
     * @param fieldValue The value to set the field to.
     */
    @Override
    public void put(int fieldIndex, Object fieldValue) {
        switch (fieldIndex) {
            case 0:
                if (fieldValue instanceof String) {
                    setEmailAddress((String) fieldValue);
                } else {
                    throw new AvroRuntimeException("Expected String for email_address, but got " + fieldValue.getClass().getName());
                }
                break;
            case 1:
                if (fieldValue instanceof String) {
                    setDepartureTime((String) fieldValue);
                } else {
                    throw new AvroRuntimeException("Expected String for departure_time, but got " + fieldValue.getClass().getName());
                }
                break;
            case 2: 
                if (fieldValue instanceof String) {
                    setDepartureAirportCode((String) fieldValue);
                } else {
                    throw new AvroRuntimeException("Expected String for departure_airport_code, but got " + fieldValue.getClass().getName());
                }
                break;
            case 3: 
                if (fieldValue instanceof String) {
                    setArrivalTime((String) fieldValue);
                } else {
                    throw new AvroRuntimeException("Expected String for arrival_time, but got " + fieldValue.getClass().getName());
                }
                break;
            case 4:
                if (fieldValue instanceof String) {
                    setArrivalAirportCode((String) fieldValue);
                } else {
                    throw new AvroRuntimeException("Expected String for arrival_airport_code, but got " + fieldValue.getClass().getName());
                }
                break;
            case 5:
                if (fieldValue instanceof Long) {
                    setFlightDuration((Long) fieldValue);
                } else if (fieldValue instanceof Integer) {
                    setFlightDuration(((Integer) fieldValue).longValue());
                } else {
                    throw new AvroRuntimeException("Expected Long for flight_duration, but got " + fieldValue.getClass().getName());
                }
                break;
            case 6:
                if (fieldValue instanceof String) {
                    setFlightNumber((String) fieldValue);
                } else {
                    throw new AvroRuntimeException("Expected String for flight_number, but got " + fieldValue.getClass().getName());
                }
                break;
            case 7:
                if (fieldValue instanceof String) {
                    setConfirmationCode((String) fieldValue);
                } else {
                    throw new AvroRuntimeException("Expected String for confirmation_code, but got " + fieldValue.getClass().getName());
                }
                break;
            case 8:
                if (fieldValue instanceof ByteBuffer) {
                    setTicketPrice(DECIMAL_CONVERSION.fromBytes((ByteBuffer) fieldValue, DECIMAL_SCHEMA, DECIMAL_TYPE));
                } else if (fieldValue instanceof BigDecimal) {
                    setTicketPrice((BigDecimal) fieldValue);
                } else {
                    throw new AvroRuntimeException("Unexpected type for ticket_price: " + fieldValue.getClass().getName());
                }
                break;
            case 9:
                if (fieldValue instanceof String) {
                    setAircraft((String) fieldValue);
                } else {
                    throw new AvroRuntimeException("Expected String for aircraft, but got " + fieldValue.getClass().getName());
                }
                break;
            case 10:
                if (fieldValue instanceof String) {
                    setBookingAgencyEmail((String) fieldValue);
                } else {
                    throw new AvroRuntimeException("Expected String for booking_agency_email, but got " + fieldValue.getClass().getName());
                }
                break;
            default: 
                throw new IndexOutOfBoundsException("Invalid field index: " + fieldIndex);
        }
    }
}