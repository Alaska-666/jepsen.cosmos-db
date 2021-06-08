package mipt.bit.utils;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosStoredProcedureProperties;
import com.azure.cosmos.models.CosmosStoredProcedureRequestOptions;
import com.azure.cosmos.models.CosmosStoredProcedureResponse;
import com.azure.cosmos.models.PartitionKey;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TransactionsExecute {
    //    private final String databaseName;
//    private final String containerName;
    private final CosmosContainer container;

    public TransactionsExecute(CosmosContainer container) {
//        this.databaseName = databaseName;
//        this.containerName = containerName;
        this.container = container;
    }

    public List<JSONObject> execute (List<Operation> operations) throws IOException {
//        String containerLink = String.format("/dbs/%s/colls/%s", databaseName, containerName);
        CosmosStoredProcedureProperties storedProcedure = new CosmosStoredProcedureProperties("spCreateToDoItems", new String(Files.readAllBytes(Paths.get("./js/spCreateToDoItems.js"))));

        //toBlocking() blocks the thread until the operation is complete and is used only for demo.
        try {
            container.getScripts().createStoredProcedure(storedProcedure, null);
        } catch (CosmosException ignored) {}
        CosmosStoredProcedureRequestOptions options = new CosmosStoredProcedureRequestOptions();
        options.setPartitionKey(new PartitionKey("key"));
        options.setScriptLoggingEnabled(true);
        CosmosStoredProcedureResponse response = container.getScripts().getStoredProcedure("spCreateToDoItems").execute(Collections.singletonList(operations), options);
        if (response.getStatusCode() != 200) {
            return Collections.emptyList();
        }
        List<JSONObject> result = new ArrayList<>();
        for (String i: response.getResponseAsString().substring(1, response.getResponseAsString().length() - 1).split("},")) {
            JSONObject obj = new JSONObject(i + "}");
            result.add(obj);
        }
        return result;
    }
}
