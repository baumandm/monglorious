# TODO

[https://docs.mongodb.com/v3.2/reference/mongo-shell/]()

## Working Commands

    db.runCommand("serverStatus")
    db.runCommand({ serverStatus: 1 })
    
    db.runCommand("dbStats")
    db.runCommand({ dbStats: 1 })
    
    db.runCommand("whatsmyuri")
    db.runCommand({ whatmyuri: 1 })
    
    db.runCommand({ "collStats": "<collection name>"})

    show dbs
    show databases
    
    show collections
    
## Expected Syntax:
    
    db.<collection>.find(<query>)
    db.<collection>.findOne(<query>)
    db.<collection>.count()
    db.<collection>.find(<query>).count