# TODO

[https://docs.mongodb.com/v3.2/reference/mongo-shell/]()

## Working Commands

    db.runCommand("serverStatus")
    db.runCommand("dbStats")
    db.runCommand({ "collStats": "<collection name>"})

## Expected Syntax:

    show dbs
    show collections
    show users
    show roles
    show profile
    show databases
    db.<collection>.find(<query>)
    db.<collection>.findOne(<query>)
    db.<collection>.count()
    db.<collection>.find(<query>).count

    db.runCommand("serverStatus")
    db.runCommand("dbStats")
    