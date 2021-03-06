package com.chongzi.es;

import com.chongzi.bean.Record;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * DataSet写入ES示例
 */
public class EsDateSetDemo {
    public static void main(String[] args) throws Exception {
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        DataSet<Record> csvInput = env
                .readCsvFile("D://projects//my-flink-demo//src//main//resources//data//olympic-athletes.csv")
                .pojoType(Record.class, "playerName", "country", "year", "game", "gold", "silver", "bronze", "total");

        //3.将数据写入到自定义的sink中（这里是es）
        Map<String, String> config = new LinkedHashMap<>();
        config.put("cluster.name", "esearch-one");
        //该配置表示批量写入ES时的记录条数
        //config.put("bulk.flush.max.actions", "1000");

        List<InetSocketAddress> transportAddresses = new ArrayList<>();
        transportAddresses.add(new InetSocketAddress(InetAddress.getByName("10.60.34.48"), 9300));
        transportAddresses.add(new InetSocketAddress(InetAddress.getByName("10.60.34.6"), 9300));
        transportAddresses.add(new InetSocketAddress(InetAddress.getByName("10.60.34.36"), 9300));

        csvInput.output(new ElasticSearchOutputFormat<>(config, transportAddresses, new ElasticsearchSinkFunction<Record>(){
            @Override
            public void process(Record element, RuntimeContext ctx, RequestIndexer indexer) {
                indexer.add(createIndexRequest(element));
            }

            public IndexRequest createIndexRequest(Record obj) {
                Map<String, Object> json = new HashMap<>();
                //将需要写入ES的字段依次添加到Map当中
                json.put("playerName", obj.getPlayerName());
                json.put("country",obj.getCountry());
                json.put("year",obj.getYear());
                json.put("game",obj.getGame());
                json.put("gold",obj.getGold());
                json.put("silver",obj.getSilver());
                json.put("bronze",obj.getBronze());
                json.put("total",obj.getTotal());
                return Requests.indexRequest()
                        .index("chongzi")
                        .type("record")
                        .source(json);
            }
        }));

        env.execute();
    }
}
