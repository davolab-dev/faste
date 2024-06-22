# FASTE

# This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.davolabsl/faste/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.davolabsl/faste)

# faste API Payload Explanation

This document provides a detailed explanation of the payload structure for the faste API advance search. The payload
is designed to facilitate queries to retrieve data from a database, with optional pagination support.

## URL : `/api/v1/reference-center/faste/advanced-search`

## Payload Structure

The payload is structured as a JSON object with the following key fields:

- Payload Structure:
  ```json
  {
    "select": [
        "<field list>"
    ],
    "from": "<entity name>",
    "logic": [
        {
            "where": "<filtering field>",
            "condition": "<filtering condition>",
            "value": "<value that passes to filter data>"
        }
    ],
    "orderBy": [
        "<field name>:<ordering condition>"
    ],
    "page":"<page number>",
    "size":"<page size>",
    "resultType": "<result type>"
  }
  ```

### 1. `select` (Array)

- Description: Specifies the fields to be retrieved from the database. To retrieve all fields, use "*" in the select field
- Format: Array of strings.
- Example 1: fetch all data
  ```json
  "select": [
        "*"
  ] 
  ```

- Example 2: select specific field list
  ```json
  "select": [
    "id",
    "suburb.code",
    "suburb.name"
  ]" 
  ```

### Available Fields for `select` Clause

The `select` clause specifies the fields to be retrieved from the database. If you're unsure about the available fields, you can include a placeholder field such as `"abc"` in the `select` array. When executing the query, the system will typically respond with an error message listing the available fields, thereby providing guidance on which fields can be used in the query.

```json
"select": [
    "abc"
]
```

### 2. from (String)

- Description: Specifies the table from which the data is to be retrieved.
- Format: String.
- Example:
  ```json
    "from": "syrcity"
  ```

### 3. logic (Array)

- Description: Specifies the search criteria for filtering the data.
- Format: Array of objects with where, condition, and value fields.
- Example:
    ```json
  "logic": [
      {
        "where": "id",
        "condition": "nequal",
        "value": 0
      }
  ]
  ```

### Possible Conditions for `where` Clause

The `where` clause specifies the condition for filtering the data. Here are the possible conditions along with their corresponding operators:

- `equal`: Equals (`=`)
- `nequal`: Not Equals (`<>`)
- `gt`: Greater Than (`>`)
- `lt`: Less Than (`<`)
- `gteq`: Greater Than or Equal To (`>=`)
- `lteq`: Less Than or Equal To (`<=`)
- `like`: Like (Pattern Matching with `%`)
- `nlike`: Not Like (Negation of Pattern Matching with `%`)
- `bt`: Between (Range Selection)

These conditions can be used to define the criteria for filtering the data in the `logic` field of the payload.

### Logical Operators for `where` Clause

If multiple conditions are used in the `logic` field, a logical operator can be specified to define the relationship between these conditions. Here are the available logical operators:

- `AND`: Logical AND operator
- `OR`: Logical OR operator

The logical operator should be included along with each condition in the `logic` field to specify how the conditions are combined.

- Example: 
  ```json
  "logic": [
    {
        "where": "id",
        "condition": "nequal",
        "value": 0,
        "logicalOperator": "AND"
    },
    {
        "where": "isActive",
        "condition": "equal",
        "value": true
    }
  ]
  ```

### 4. orderBy (Array)
- Description: Specifies the field(s) to sort the results by.
- Format: Array of strings with field names and sorting direction.
- Example:
    ```json
  "orderBy": [
    "suburb.code:ASC"
  ]
    ```

### Ordering Conditions for `orderBy` Clause

The `orderBy` clause specifies the field(s) by which the results are ordered. Here are the available ordering conditions:

- `ASC`: Ascending order
- `DESC`: Descending order

These conditions can be used to define the sorting order in the `orderBy` field of the payload.


### 5. page (Integer)
- Description: Specifies the page number for pagination.
- Format: Integer.
- Example:
    ```json
  "page": 0
    ```

### 6. size (Integer)
- Description: Specifies the number of records per page for pagination.
- Format: Integer.
- Example:
    ```json
  "size": 1
    ```

### 7. resultType (String)
- Description: Specifies whether the expected result is a single record (S) or multiple records (M).
- Format: String.
- Example:
    ```json
  "resultType": "M"
    ```


