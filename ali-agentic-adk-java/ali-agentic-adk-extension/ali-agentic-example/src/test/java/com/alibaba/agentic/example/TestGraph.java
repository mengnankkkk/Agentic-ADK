package com.alibaba.agentic.example;

import com.alibaba.agentic.core.engine.node.FlowCanvas;
import com.alibaba.agentic.core.engine.node.sub.ToolFlowNode;
import com.alibaba.agentic.core.executor.InvokeMode;
import com.alibaba.agentic.core.executor.Request;
import com.alibaba.agentic.core.executor.Result;
import com.alibaba.agentic.core.executor.SystemContext;
import com.alibaba.agentic.core.runner.Runner;
import com.alibaba.agentic.core.tools.BaseTool;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * DESCRIPTION
 *
 * @author baliang.smy
 * @date 2025/8/4 10:34
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Application.class })
@ActiveProfiles("testing")
public class TestGraph {

    @Test
    public void testSimpleSequentialFlow() {
        System.out.println("========== testSimpleSequentialFlow ==========");
        
        FlowCanvas flowCanvas = new FlowCanvas();

        ToolFlowNode step1 = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "step1";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Executing Step 1: Data Collection");
                return Flowable.just(Map.of("step1_result", "Data collected", "count", 100));
            }
        });
        step1.setId("step1Node");

        ToolFlowNode step2 = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "step2";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Executing Step 2: Data Processing");
                return Flowable.just(Map.of("step2_result", "Data processed", "quality", 95));
            }
        });
        step2.setId("step2Node");

        ToolFlowNode step3 = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "step3";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Executing Step 3: Final Output");
                return Flowable.just(Map.of("final_result", "Process completed successfully"));
            }
        });
        step3.setId("step3Node");

        step1.next(step2);
        step2.next(step3);

        flowCanvas.setRoot(step1);

        Flowable<Result> flowable = new Runner().run(flowCanvas, new Request()
                .setInvokeMode(InvokeMode.SYNC)
                .setParam(Map.of("input", "test data")));

        flowable.blockingIterable().forEach(event -> 
            System.out.println("Result: " + event.getData())
        );

        System.out.println("✅ Sequential flow completed\n");
    }

    @Test
    public void testToolFlowNodeWithParameters() {
        System.out.println("========== testToolFlowNodeWithParameters ==========");
        
        FlowCanvas flowCanvas = new FlowCanvas();

        ToolFlowNode calculator = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "calculator";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                int a = (int) args.getOrDefault("a", 0);
                int b = (int) args.getOrDefault("b", 0);
                int result = a + b;
                System.out.println("Calculating: " + a + " + " + b + " = " + result);
                return Flowable.just(Map.of("result", result, "operation", "addition"));
            }
        });
        calculator.setId("calculatorNode");

        flowCanvas.setRoot(calculator);

        Flowable<Result> flowable = new Runner().run(flowCanvas, new Request()
                .setInvokeMode(InvokeMode.SYNC)
                .setParam(Map.of("a", 10, "b", 20)));

        flowable.blockingIterable().forEach(event -> 
            System.out.println("Calculation result: " + event.getData())
        );

        System.out.println("✅ Tool with parameters completed\n");
    }

    @Test
    public void testMultipleToolNodes() {
        System.out.println("========== testMultipleToolNodes ==========");
        
        FlowCanvas flowCanvas = new FlowCanvas();

        ToolFlowNode dataFetcher = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "dataFetcher";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Fetching data from source...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return Flowable.just(Map.of("data", "User data", "records", 1000));
            }
        });
        dataFetcher.setId("dataFetcherNode");

        ToolFlowNode dataTransformer = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "dataTransformer";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Transforming data format...");
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return Flowable.just(Map.of("transformed_data", "JSON format", "status", "success"));
            }
        });
        dataTransformer.setId("dataTransformerNode");

        ToolFlowNode dataSaver = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "dataSaver";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Saving data to database...");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return Flowable.just(Map.of("saved", true, "location", "database", "timestamp", System.currentTimeMillis()));
            }
        });
        dataSaver.setId("dataSaverNode");

        dataFetcher.next(dataTransformer);
        dataTransformer.next(dataSaver);

        flowCanvas.setRoot(dataFetcher);

        Flowable<Result> flowable = new Runner().run(flowCanvas, new Request()
                .setInvokeMode(InvokeMode.SYNC)
                .setParam(Map.of("source", "user_database")));

        flowable.blockingIterable().forEach(event -> 
            System.out.println("Pipeline result: " + event.getData())
        );

        System.out.println("✅ Multiple tool nodes pipeline completed\n");
    }
}
