# SimpleRemote — Server API Specification

This document describes the HTTP API that SimpleRemote expects from a 1C:Enterprise server.

## Base URL

```
{scheme}://{server}/{databaseName}/hs/rc/
```

- `scheme` — `http` or `https` (cleartext HTTP is supported)
- `server` — hostname and optional port (e.g. `192.168.1.10:8080`)
- `databaseName` — 1C information base name

All endpoints use **POST** method and are relative to this base URL.

## Authentication

1. **Basic Auth** — every request includes an `Authorization: Basic ...` header with the 1C user credentials.
2. **Token-based** — the initial `check` call returns a session `token`. Subsequent requests include the token in the URL path. On `401`, the app automatically re-authenticates (up to 3 retries).

## Endpoints Index

| # | Operation | Endpoint | Request `type` | Description |
|---|-----------|----------|-----------------|-------------|
| 1 | [Check / Auth](#1-check--authenticate) | `pst/{userId}` | `check` | Authenticate and get session token + user options |
| 2 | [List Documents](#2-list-documents) | `pst/{token}` | `documents` | Get documents of a given type |
| 3 | [Document Content](#3-document-content) | `pst/{token}` | `documentContent` | Get line items of a document |
| 4 | [Barcode Lookup](#4-barcode-lookup) | `pst/{token}` | `barcode` | Find a product by barcode value |
| 5 | [Catalog](#5-catalog) | `pst/{token}` | `catalog` | Browse product catalog |
| 6 | [Save Document](#6-save-document) | `pst/{token}` | `saveDocument` | Save document with modified lines |
| 7 | [Lock Document](#7-lock-document) | `pst/{token}` | `lockDocument` | Acquire edit lock on a document |
| 8 | [Unlock Document](#8-unlock-document) | `pst/{token}` | `unlockDocument` | Release edit lock on a document |

All endpoints share the same URL pattern `pst/{token}` (except `check` which uses the user ID). The server distinguishes operations by the `type` field in the JSON body.

---

## Common Response Envelope

Every response follows this structure:

```json
{
  "result": "ok",
  "message": "",
  "token": "session-token-string",
  "data": [ ... ]
}
```

- `result` — `"ok"` on success, other values on failure
- `message` — error description (empty on success)
- `token` — current session token (may be refreshed)
- `data` — array of result objects (type depends on operation)

---

## 1. Check / Authenticate

Authenticates the user and returns session configuration.

**Endpoint:** `POST pst/{userId}`

**Request:**
```json
{
  "userID": "user-guid",
  "type": "check",
  "data": ""
}
```

**Response:**
```json
{
  "result": "ok",
  "message": "",
  "token": "abc123",
  "data": [
    {
      "write": true,
      "read": true,
      "mode": "input",
      "loadImages": false,
      "confirmWithScan": false,
      "user": "Warehouse Operator",
      "catalog": [
        { "code": "goods", "description": "Products" }
      ],
      "document": [
        { "code": "sales", "description": "Sales Orders" },
        { "code": "inventory", "description": "Inventory Count" }
      ]
    }
  ]
}
```

**User options fields:**

| Field | Type | Description |
|-------|------|-------------|
| `write` | boolean | User can modify documents |
| `read` | boolean | User can read documents |
| `mode` | string | App mode: `"input"`, `"inventory"`, `"assemble"` |
| `loadImages` | boolean | Load product images |
| `confirmWithScan` | boolean | Allow confirming quantity by scanning |
| `user` | string | Display name |
| `catalog` | array | Available catalog types |
| `document` | array | Available document types |

---

## 2. List Documents

Returns documents of a specified type.

**Endpoint:** `POST pst/{token}`

**Request:**
```json
{
  "userID": "user-guid",
  "type": "documents",
  "data": {
    "type": "sales"
  },
  "filter": [
    { "meta": "warehouse", "type": "ref", "name": "Warehouse", "code": "WH-001", "value": "Main" }
  ]
}
```

The `filter` array is optional. Each filter item has:

| Field | Type | Description |
|-------|------|-------------|
| `meta` | string | Filter metadata identifier |
| `type` | string | Filter value type (e.g. `"ref"`, `"string"`) |
| `name` | string | Display name |
| `description` | string | Human-readable description |
| `code` | string | Filter value code |
| `value` | string | Filter value |

**Response:**
```json
{
  "result": "ok",
  "message": "",
  "token": "abc123",
  "data": [
    {
      "guid": "doc-guid-1",
      "type": "sales",
      "isProcessed": 0,
      "isDeleted": 0,
      "title": "Sales Order",
      "number": "00042",
      "date": "2025-01-15",
      "company": "My Company",
      "contractor": "Customer LLC",
      "warehouse": "Main Warehouse",
      "sum": "1500.00",
      "checked": false,
      "notes": "",
      "locked": false,
      "lockedBy": "",
      "currency": "UAH",
      "placesCollected": "0",
      "field1": { "meta": "", "type": "", "name": "", "description": "", "code": "" },
      "field2": { "meta": "", "type": "", "name": "", "description": "", "code": "" },
      "field3": { "meta": "", "type": "", "name": "", "description": "", "code": "" },
      "field4": { "meta": "", "type": "", "name": "", "description": "", "code": "" }
    }
  ],
  "filter": [
    { "meta": "warehouse", "type": "ref", "name": "Warehouse", "description": "Filter by warehouse" }
  ]
}
```

The response `filter` array defines the available filter schema for this document type (returned with empty `code`/`value`).

---

## 3. Document Content

Returns line items for a specific document.

**Endpoint:** `POST pst/{token}`

**Request:**
```json
{
  "userID": "user-guid",
  "type": "documentContent",
  "data": {
    "type": "sales",
    "guid": "doc-guid-1"
  }
}
```

**Response:**
```json
{
  "result": "ok",
  "message": "",
  "token": "abc123",
  "data": [
    {
      "line": 1,
      "code": "SKU-001",
      "code2": "4820000001",
      "code3": "item-guid",
      "art": "ART-001",
      "description": "Widget A",
      "unit": "pcs",
      "quantity": "10",
      "rest": "25",
      "price": "150.00",
      "sum": "1500.00",
      "image": "",
      "collect": "0",
      "notes": "",
      "checked": false,
      "place": []
    }
  ]
}
```

**Content line fields:**

| Field | Type | Description |
|-------|------|-------------|
| `line` | int | Line number |
| `code` | string | Product code (SKU) |
| `code2` | string | Barcode |
| `code3` | string | Product GUID |
| `art` | string | Article number |
| `description` | string | Product name |
| `unit` | string | Unit of measure |
| `quantity` | string | Expected quantity |
| `rest` | string | Stock remainder |
| `price` | string | Unit price |
| `sum` | string | Line total |
| `image` | string | Image reference |
| `collect` | string | Collected quantity (editable) |
| `notes` | string | Line notes (editable) |
| `checked` | boolean | Whether line is confirmed |
| `place` | array | Placement info: `[{ "quantity": 5, "code": "A1" }]` |

---

## 4. Barcode Lookup

Finds a product by barcode within a document context.

**Endpoint:** `POST pst/{token}`

**Request:**
```json
{
  "userID": "user-guid",
  "type": "barcode",
  "data": {
    "type": "sales",
    "guid": "doc-guid-1",
    "value": "4820000001"
  }
}
```

**Response:**
```json
{
  "result": "ok",
  "message": "",
  "token": "abc123",
  "data": [
    {
      "type": "goods",
      "id": "product-guid",
      "isGroup": 0,
      "code": "SKU-001",
      "description": "Widget A",
      "notes": "",
      "art": "ART-001",
      "unit": "pcs",
      "groupName": "Widgets",
      "groupCode": "GRP-01",
      "rest": 25.0,
      "price": 150.0,
      "barcode": "4820000001",
      "contentItem": null
    }
  ]
}
```

The `contentItem` field is optional and may contain a pre-filled `Content` object if the product is already in the document.

---

## 5. Catalog

Browses the product catalog with optional group navigation and search.

**Endpoint:** `POST pst/{token}`

**Request:**
```json
{
  "userID": "user-guid",
  "type": "catalog",
  "data": {
    "type": "goods",
    "group": "",
    "documentGUID": "doc-guid-1",
    "searchFilter": ""
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | Catalog type code (from user options) |
| `group` | string | Parent group code (empty for root) |
| `documentGUID` | string | Document context for stock/price data |
| `searchFilter` | string | Text search query (empty to list all) |

**Response:**
```json
{
  "result": "ok",
  "message": "",
  "token": "abc123",
  "data": [
    {
      "id": "item-guid",
      "type": "goods",
      "isGroup": 1,
      "code": "GRP-01",
      "description": "Widgets",
      "art": "",
      "unit": "",
      "groupName": "",
      "groupCode": "",
      "rest": 0.0,
      "price": 0.0
    },
    {
      "id": "item-guid-2",
      "type": "goods",
      "isGroup": 0,
      "code": "SKU-001",
      "description": "Widget A",
      "art": "ART-001",
      "unit": "pcs",
      "groupName": "Widgets",
      "groupCode": "GRP-01",
      "rest": 25.0,
      "price": 150.0
    }
  ]
}
```

Items with `isGroup: 1` are folders; the app navigates into them by passing their `code` as the `group` parameter.

---

## 6. Save Document

Saves a document with its modified line items back to 1C.

**Endpoint:** `POST pst/{token}`

**Request:**
```json
{
  "userID": "user-guid",
  "type": "saveDocument",
  "data": {
    "guid": "doc-guid-1",
    "type": "sales",
    "number": "00042",
    "date": "2025-01-15",
    "notes": "Updated by SimpleRemote",
    "lines": [
      {
        "line": 1,
        "code": "SKU-001",
        "collect": "10",
        "checked": true,
        "notes": "",
        "encodedImage": ""
      }
    ]
  }
}
```

The `data` field contains the full `Document` object. Only lines with `modified: true` are typically included in the `lines` array.

**Response:**
```json
{
  "result": "ok",
  "message": "",
  "token": "abc123",
  "data": [
    {
      "saved": "ok",
      "error": ""
    }
  ]
}
```

- `saved` — `"ok"` on success
- `error` — error message if save failed

---

## 7. Lock Document

Acquires an edit lock on a document to prevent concurrent modifications.

**Endpoint:** `POST pst/{token}`

**Request:**
```json
{
  "userID": "user-guid",
  "type": "lockDocument",
  "data": {
    "type": "sales",
    "guid": "doc-guid-1"
  }
}
```

**Response:**
```json
{
  "result": "ok",
  "message": "",
  "token": "abc123",
  "data": [
    {
      "locked": "ok",
      "error": ""
    }
  ]
}
```

---

## 8. Unlock Document

Releases the edit lock on a document.

**Endpoint:** `POST pst/{token}`

**Request:**
```json
{
  "userID": "user-guid",
  "type": "unlockDocument",
  "data": {
    "type": "sales",
    "guid": "doc-guid-1"
  }
}
```

**Response:**
```json
{
  "result": "ok",
  "message": "",
  "token": "abc123",
  "data": [
    {
      "locked": "ok",
      "error": ""
    }
  ]
}
```

---

## Data Flow Summary

```
App                                     1C Server
 │                                         │
 ├─── POST pst/{userId}  [check] ────────►│  Authenticate
 │◄── token + user options ───────────────┤
 │                                         │
 ├─── POST pst/{token}  [documents] ─────►│  List documents
 │◄── document list + filter schema ──────┤
 │                                         │
 ├─── POST pst/{token}  [lockDocument] ──►│  Lock for editing
 │◄── lock result ────────────────────────┤
 │                                         │
 ├─── POST pst/{token}  [documentContent]►│  Get line items
 │◄── content lines ──────────────────────┤
 │                                         │
 ├─── POST pst/{token}  [barcode] ───────►│  Scan barcode
 │◄── product info ───────────────────────┤
 │                                         │
 ├─── POST pst/{token}  [catalog] ───────►│  Browse catalog
 │◄── catalog items ──────────────────────┤
 │                                         │
 ├─── POST pst/{token}  [saveDocument] ──►│  Save changes
 │◄── save result ────────────────────────┤
 │                                         │
 ├─── POST pst/{token}  [unlockDocument]─►│  Release lock
 │◄── unlock result ──────────────────────┤
```
