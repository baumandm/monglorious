# TODO

[https://docs.mongodb.com/v3.2/reference/mongo-shell/]()

- Regex queries: 
    - Tests for regex by string (already works)
    - Support for /regex/ literals  

## Working Commands

    show dbs
    show databases
    show collections
    
    db.runCommand("serverStatus")
    db.runCommand({ serverStatus: 1 })
    
    db.runCommand("dbStats")
    db.runCommand({ dbStats: 1 })
    
    db.runCommand("whatsmyuri")
    db.runCommand({ whatmyuri: 1 })
    
    db.runCommand({ "collStats": "<collection name>"})
    
    db.<collection>.find(<query>)
    db.<collection>.findOne(<query>)
    db.<collection>.count()
    
    db.<collection>.find(<query>).count()
    
    db.<collection.find().sort().skip().limit()

    
