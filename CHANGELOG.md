# CHANGELOG

## 0.7.0

- Add db.collection.insert() and db.collection.insertOne() functions

## 0.6.0

- Add Date() and ISODate() functions 

## 0.5.0

- Add db.collection.aggregate()

## 0.4.0

- Add regex support to db.collection.find()
- Add db.collection.stats(), dataSize(), storageSize(), totalIndexSize(), getIndexes(),
- Add db.collection.find().batchSize()

## 0.3.0

- Add ObjectId() support

## 0.2.1

- Renamed grammar file to avoid potential conflicts

## 0.2.0

- Added support for cursor functions to be chained (in any order) after db.collection.find():
    - sort()
    - skip()
    - limit()
