// Initialize MongoDB database and collections
db = db.getSiblingDB('prod_clearance');

// Create collection with validation schema
db.createCollection('group_process_status', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['type', 'product_id', 'status', 'updated_dt'],
      properties: {
        _id: { bsonType: 'objectId' },
        type: { bsonType: 'string', enum: ['PROD', 'STAGING', 'DEV'] },
        product_id: { bsonType: 'string' },
        status: { bsonType: 'string', enum: ['R', 'P', 'C', 'E'] },
        updated_dt: { bsonType: 'date' },
        aws_request_id: { bsonType: 'string' },
        error_message: { bsonType: 'string' },
        retry_count: { bsonType: 'int' }
      }
    }
  }
});

// Create indexes for better query performance
db.group_process_status.createIndex({ status: 1 });
db.group_process_status.createIndex({ type: 1, status: 1 });
db.group_process_status.createIndex({ product_id: 1 });
db.group_process_status.createIndex({ updated_dt: -1 });

print('MongoDB initialization completed successfully!');
print('Collection: group_process_status');
print('Indexes created for: status, type+status, product_id, updated_dt');