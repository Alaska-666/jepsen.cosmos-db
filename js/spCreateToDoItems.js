function transactionsExecute(operations) {
    let context = getContext();
    let container = context.getCollection();
    let response = context.getResponse();

    let result = []
    let createdItems = {}

    function run(i) {
        if (i >= operations.length) {
            return
        }
        let op = operations[i];
        console.log(JSON.stringify(op))
        var filterQuery =
            {
                'query' : 'SELECT * FROM JepsenTestContainer p where p.id = @id',
                'parameters' : [{'name':'@id', 'value':op.key}]
            };

        var accept = container.queryDocuments(container.getSelfLink(), filterQuery, {},
            function (err, items, responseOptions) {
                if (err) throw new Error("Error" + err.message);

                if (op.type === "append") {
                    if (items.length === 0 && !(op.key in createdItems)) {
                        let accept2 = container.createDocument(container.getSelfLink(), {id: op.key, values: [op.value], key: "key"},{ disableAutomaticIdGeneration: true }, function (err, d) {
                            if (err) throw JSON.stringify(op);
                            result.push(op);
                            createdItems[op.key] = [op.value]
                            run(i+1);
                        });
                        if (!accept2) throw "Unable to replace found list in transaction";
                        return;
                    }
                    if (items.length !== 1 && !(op.key in createdItems)) throw "Unable to find both names append";


                    if (items.length !== 1) {
                        createdItems[op.key].push(op.value)
                        let accept2 = container.upsertDocument(container.getSelfLink(), {id: op.key, values: createdItems[op.key], key: "key"}, { disableAutomaticIdGeneration: true }, function (err, d) {
                            if (err) throw err;
                            result.push(op);
                            run(i+1);
                        });
                        if (!accept2) throw "Unable to replace found list in transaction";
                        return;
                    } else {
                        let foundList = items[0];
                        foundList.values.push(op.value)
                        let accept2 = container.replaceDocument(foundList._self, foundList, function (err, d) {
                            if (err) throw err;
                            result.push(op);
                            run(i+1);
                        });
                        if (!accept2) throw "Unable to replace found list in transaction";
                        return;
                    }
                }

                if (items.length === 0 && !(op.key in createdItems)) {
                    op.readResult = [];
                } else {
                    if (items.length !== 1 && !(op.key in createdItems)) throw "Unable to find both names r";
                    else {
                        if (items.length === 1) {
                            op.readResult = items[0].values;
                        } else {
                            op.readResult = [...createdItems[op.key]]
                        }
                    }
                }

                result.push(op);
                run(i+1);
            });

        if (!accept) throw "Unable to read, abort";

    }
    run(0)
    response.setBody(result);
}