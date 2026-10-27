package com.rbdip.bookstore.order;


public record CustomerName(String firstName, String lastName) {

    private static final String WHITESPACE = "\\s+";
    private static final String SINGLE_SPACE = " ";

    public static CustomerName parse(String fullName) {
        String normalized = fullName.trim().replaceAll(WHITESPACE, SINGLE_SPACE);
        int separator = normalized.indexOf(SINGLE_SPACE);
        if (separator < 0) {
            return new CustomerName(normalized, "");
        }
        return new CustomerName(
                normalized.substring(0, separator),
                normalized.substring(separator + SINGLE_SPACE.length()));
    }

    public static CustomerName of(String firstName, String lastName) {
        return new CustomerName(firstName, lastName);
    }

    public String fullName() {
        return lastName.isBlank() ? firstName : firstName + SINGLE_SPACE + lastName;
    }
}
