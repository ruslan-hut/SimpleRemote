# SimpleRemote - Functional Specification

## Overview

SimpleRemote is a mobile application for warehouse and retail staff to view, edit, and process documents from a 1C:Enterprise database. The application operates without a local database - all data is requested on-demand from the 1C server via HTTP.

### Primary Use Cases

- **Quick Receipt Compilation**: Retail floor staff pre-assemble shopping baskets/receipts that customers complete at checkout
- **Inventory Management**: Warehouse and retail staff conduct physical inventory counts with real-time server communication
- **Document Processing**: Edit and save documents (invoices, receipts, orders) back to the 1C database

---

## Core Features

### 1. Document Management

#### Document Browsing
- View lists of documents organized by type (types defined on server)
- Filter documents by:
  - Document number
  - Contractor/Supplier name
  - Warehouse/Storage location
  - Date
- Active filters displayed in a banner above the list

#### Document Display
Documents show status indicators:
- Processed / Unprocessed / Deleted states
- "Checked" badge for completed documents
- "Repeated" indicator for modified documents
- Unsaved changes indicator

#### Document Detail View
Two-tab interface:
- **Parameters Tab**: Header information including number, date, contractor, up to 4 custom configurable fields, total sum, currency, notes
- **Products Tab**: Line items in the document

### 2. Line Item Management

When edit mode is enabled, users can:
- Modify collected/received quantities
- Add notes to individual items
- Mark items as complete (checked)
- View product details: code, description, unit, original quantity, available stock, price
- Capture and attach product photos
- See visual indicators for modified items

### 3. Barcode Scanning

#### Hardware Scanner Support
- Automatic detection of connected physical barcode scanners
- Recognized devices: Honeywell, Zebra, generic USB/Bluetooth scanners
- Real-time capture of barcode data
- Audible feedback on successful scan

#### Camera-Based Scanning
- Real-time barcode detection via device camera
- Two modes:
  - **Barcode mode**: Scan to find/add products
  - **Photo mode**: Capture product images

#### Barcode Processing Behavior
- Lookup product by barcode against server
- For existing items: increment collected quantity
- For new items: add from catalog if available
- Notification if barcode has no match
- Optional confirmation mode: require re-scan to confirm quantity increment

### 4. Catalog Browsing

- Browse product catalogs by type
- Hierarchical navigation through product groups
- View product details: article code, description, group, price, available stock
- Add products to active document (quantity = 1)

### 5. Item Placement Tracking

- Assign items to storage locations/shelves/bins
- Track quantities per location
- Add locations via barcode scanning
- Edit or delete location entries
- Support multiple locations per item

### 6. Document Saving

- Save complete document with all modifications to server
- Server-side validation performed
- Success/error feedback displayed
- Warning prompt when navigating away from unsaved changes

### 7. Connection Management

- Support for multiple server connections
- Connection settings:
  - Description (alias)
  - Server address
  - Database name
  - Username and password
  - Auto-connect option
- Add, edit, and delete connections
- Demo connection available for testing
- Display unique user ID per connection

---

## User Flows

### Main Navigation
```
App Launch
    ↓
Select Document Type
    ↓
Document List (with optional filters)
    ↓
Document Detail (Parameters / Products tabs)
    ↓
Item Edit or Item Placement
    ↓
Save and return
```

### Barcode Scanning Flow
1. Hardware scanner or camera detects barcode
2. Product lookup performed on server
3. If found in document: quantity incremented
4. If not found: notification displayed
5. Item scrolls into view, list updates

### Add from Catalog Flow
1. User taps "Add Item" in document
2. Select catalog type (if multiple)
3. Navigate through product groups
4. Select product
5. Product added to document with quantity = 1
6. Return to document detail

---

## Entity Structures

### Document
| Field | Description |
|-------|-------------|
| guid | Unique identifier |
| type | Document type |
| number | Document number |
| date | Document date |
| company | Organization |
| contractor | Supplier/Customer name |
| warehouse | Warehouse/Location |
| sum | Total amount |
| currency | Currency code |
| checked | Completion status |
| notes | User notes |
| field1-4 | Custom configurable fields |
| isProcessed | Processing status (0/1) |
| isDeleted | Deletion status (0/1) |
| repeated | Modification indicator |
| placesCollected | Number of warehouse locations |
| lines | List of line items |
| modified | Has unsaved changes |

### Line Item (Content)
| Field | Description |
|-------|-------------|
| line | Line number |
| code | Product code |
| code2 | Barcode |
| code3 | Alternative identifier |
| art | Product article/SKU |
| description | Product name |
| unit | Unit of measure |
| quantity | Required/planned quantity |
| rest | Available stock |
| price | Unit price |
| sum | Total price |
| image | Server image identifier |
| collect | Collected quantity (editable) |
| notes | Line notes (editable) |
| checked | Completion status (editable) |
| modified | User modified flag |
| userImage | Captured photo filename |
| place | List of warehouse locations |

### Place
| Field | Description |
|-------|-------------|
| code | Location/bin code |
| quantity | Items at this location |

### Product / Catalog Item
| Field | Description |
|-------|-------------|
| id | Unique identifier |
| type | Product type |
| isGroup | Category (1) or product (0) |
| code | Product code |
| description | Product name |
| art | Article/SKU |
| unit | Unit of measure |
| groupName | Parent group name |
| groupCode | Parent group code |
| rest | Available stock |
| price | Unit price |
| barcode | Barcode value |

### Document Field (Custom)
| Field | Description |
|-------|-------------|
| name | Field code |
| description | Display label |
| value | Field value |
| type | Field type |

### Filter Parameters
| Field | Description |
|-------|-------------|
| documentNumber | Filter by document number |
| contractor | Filter by contractor |
| warehouse | Filter by warehouse |
| date | Filter by date (yyyy-MM-dd) |

### Connection Settings
| Field | Description |
|-------|-------------|
| guid | Unique connection ID |
| description | Connection alias |
| serverAddress | Server hostname/IP |
| databaseName | 1C database name |
| user | Login username |
| password | Login password |
| isCurrent | Active connection flag |
| autoConnect | Auto-connect on launch |

### User Options
| Field | Description |
|-------|-------------|
| userId | User identifier |
| token | Session token |
| confirmWithScan | Require barcode confirmation |
| mode | Operating mode (collect/placement/edit) |
| loadImages | Fetch product images |

---

## Business Rules

### Document Completion
- Item marked "checked" when collected quantity >= required quantity
- Document marked "checked" when all line items are checked
- Automatic propagation: checking last item marks document complete

### Barcode Processing
- Minimum barcode length: 10 characters
- 60ms timeout between keystrokes distinguishes separate scans
- Each scan increments quantity by 1 (unless confirmation mode enabled)
- Confirmation mode requires re-scan to increment (prevents duplicates)
- Barcode matched against code2 field in line items

### Modification Tracking
- Line modified flag set when user edits quantity, notes, or checked status
- Document modified flag set when any line or header field changed
- Visual indicator (star icon) shows modified lines
- Unsaved changes warning on back navigation

### Placement Mode
- Barcode treated as warehouse location code
- Existing location: increment quantity
- New location: add entry with quantity = 1

### Catalog Operations
- Items with isGroup = 1 are navigable groups
- Items with isGroup = 0 are selectable products
- Added products start with collect = 1, checked = true, modified = true

### Image Management
- Product images retrieved from server via image ID
- User-captured photos stored locally, referenced by filename
- Images displayed as thumbnails with fullscreen preview on tap

### Connection Validation
- Server address accepts hostname, IP, or URL
- Auto-prefix with http:// if no protocol specified
- Maximum 3 token refresh attempts before failure
- Demo connection always available as fallback

---

## Server Operations

### Authentication
- Initial check request establishes session and returns token
- All subsequent requests use token for authentication
- Token refresh mechanism with retry logic

### Available Operations

| Operation | Purpose |
|-----------|---------|
| Check | Authenticate and get session token |
| Get Document Types | Retrieve available document categories |
| Get Documents | Fetch document list by type with filters |
| Get Document Content | Fetch line items for specific document |
| Lookup Barcode | Find product by barcode value |
| Get Catalog | Browse product catalog by type and group |
| Save Document | Persist document changes to server |

### Save Document Payload
Complete document snapshot including:
- All header fields
- All line items with modifications
- Placement/location data
- User notes and checked status
- Captured images (encoded)

---

## Screen Summary

| Screen | Purpose |
|--------|---------|
| Select Document Type | Choose document category to browse |
| Document List | Browse and filter documents |
| Document Detail | View/edit document header and line items |
| Item Edit | Edit individual line item details |
| Item Placement | Assign warehouse locations to item |
| Catalog | Browse and add products from catalog |
| Camera | Scan barcodes or capture photos |
| Connections | Manage server connections |
| Select Catalog Type | Choose catalog category to browse |
