package com.alibaba.agentic.example;

import com.alibaba.agentic.core.engine.node.FlowCanvas;
import com.alibaba.agentic.core.engine.node.sub.ConditionalContainer;
import com.alibaba.agentic.core.engine.node.sub.ToolFlowNode;
import com.alibaba.agentic.core.engine.utils.DelegationUtils;
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

import java.util.List;
import java.util.Map;

/**
 * Advanced graph workflow examples
 *
 * @author Libres-coder
 * @date 2025/10/18
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Application.class })
@ActiveProfiles("testing")
public class TestGraphAdvanced {

    @Test
    public void testConditionalBranching() {
        System.out.println("========== testConditionalBranching ==========");
        
        ToolFlowNode decisionNode = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "decisionMaker";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                int value = (int) args.getOrDefault("value", 0);
                String decision = value > 50 ? "high" : "low";
                System.out.println("Decision: value " + value + " is " + decision);
                return Flowable.just(Map.of("decision", decision, "value", value));
            }
        });
        decisionNode.setId("decisionNode");

        ToolFlowNode highPathNode = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "highPathProcessor";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Processing HIGH value path");
                return Flowable.just(Map.of("path", "high", "result", "High value processing completed"));
            }
        });
        highPathNode.setId("highPathNode");

        ToolFlowNode lowPathNode = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "lowPathProcessor";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Processing LOW value path");
                return Flowable.just(Map.of("path", "low", "result", "Low value processing completed"));
            }
        });
        lowPathNode.setId("lowPathNode");

        FlowCanvas flowCanvas = new FlowCanvas();
        flowCanvas.setRoot(decisionNode.nextOnCondition(List.of(
            new ConditionalContainer() {
                @Override
                public Boolean eval(SystemContext systemContext) {
                    Map<String, Object> result = DelegationUtils.getResultOfNode(systemContext, "decisionNode", Map.class);
                    return "high".equals(result.get("decision"));
                }
            }.setFlowNode(highPathNode),
            new ConditionalContainer() {
                @Override
                public Boolean eval(SystemContext systemContext) {
                    Map<String, Object> result = DelegationUtils.getResultOfNode(systemContext, "decisionNode", Map.class);
                    return "low".equals(result.get("decision"));
                }
            }.setFlowNode(lowPathNode)
        )));

        System.out.println("\nTest with value = 75 (should take HIGH path)");
        Flowable<Result> flowable = new Runner().run(flowCanvas, new Request()
                .setInvokeMode(InvokeMode.SYNC)
                .setParam(Map.of("value", 75)));
        flowable.blockingIterable().forEach(event -> 
            System.out.println("Result: " + event.getData())
        );

        System.out.println("✅ Conditional branching completed\n");
    }

    @Test
    public void testComplexWorkflow() {
        System.out.println("========== testComplexWorkflow ==========");
        
        FlowCanvas flowCanvas = new FlowCanvas();

        ToolFlowNode inputValidator = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "inputValidator";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                String input = (String) args.getOrDefault("input", "");
                boolean isValid = !input.isEmpty() && input.length() > 5;
                System.out.println("Validating input: '" + input + "' -> " + (isValid ? "VALID" : "INVALID"));
                return Flowable.just(Map.of("isValid", isValid, "input", input));
            }
        });
        inputValidator.setId("validatorNode");

        ToolFlowNode processValid = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "validProcessor";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Processing valid input...");
                return Flowable.just(Map.of("status", "processed", "result", "Success"));
            }
        });
        processValid.setId("processValidNode");

        ToolFlowNode handleInvalid = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "errorHandler";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Handling invalid input...");
                return Flowable.just(Map.of("status", "error", "message", "Input validation failed"));
            }
        });
        handleInvalid.setId("handleInvalidNode");

        ToolFlowNode finalStep = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "finalizer";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Finalizing workflow...");
                return Flowable.just(Map.of("completed", true, "timestamp", System.currentTimeMillis()));
            }
        });
        finalStep.setId("finalizerNode");

        processValid.next(finalStep);
        handleInvalid.next(finalStep);

        flowCanvas.setRoot(inputValidator.nextOnCondition(List.of(
            new ConditionalContainer() {
                @Override
                public Boolean eval(SystemContext systemContext) {
                    Map<String, Object> result = DelegationUtils.getResultOfNode(systemContext, "validatorNode", Map.class);
                    return (Boolean) result.get("isValid");
                }
            }.setFlowNode(processValid),
            new ConditionalContainer() {
                @Override
                public Boolean eval(SystemContext systemContext) {
                    Map<String, Object> result = DelegationUtils.getResultOfNode(systemContext, "validatorNode", Map.class);
                    return !(Boolean) result.get("isValid");
                }
            }.setFlowNode(handleInvalid)
        )));

        System.out.println("\nTest with valid input");
        Flowable<Result> flowable = new Runner().run(flowCanvas, new Request()
                .setInvokeMode(InvokeMode.SYNC)
                .setParam(Map.of("input", "This is a valid input")));
        flowable.blockingIterable().forEach(event -> 
            System.out.println("Result: " + event.getData())
        );

        System.out.println("✅ Complex workflow completed\n");
    }

    @Test
    public void testMultiBranchDecision() {
        System.out.println("========== testMultiBranchDecision ==========");
        
        FlowCanvas flowCanvas = new FlowCanvas();

        ToolFlowNode classifier = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "classifier";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                int score = (int) args.getOrDefault("score", 0);
                String category;
                if (score >= 90) {
                    category = "excellent";
                } else if (score >= 70) {
                    category = "good";
                } else {
                    category = "needs_improvement";
                }
                System.out.println("Score " + score + " classified as: " + category);
                return Flowable.just(Map.of("category", category, "score", score));
            }
        });
        classifier.setId("classifierNode");

        ToolFlowNode excellentHandler = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "excellentHandler";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Handling EXCELLENT performance");
                return Flowable.just(Map.of("action", "reward", "message", "Outstanding!"));
            }
        });
        excellentHandler.setId("excellentNode");

        ToolFlowNode goodHandler = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "goodHandler";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Handling GOOD performance");
                return Flowable.just(Map.of("action", "encourage", "message", "Well done!"));
            }
        });
        goodHandler.setId("goodNode");

        ToolFlowNode improvementHandler = new ToolFlowNode(null, new BaseTool() {
            @Override
            public String name() {
                return "improvementHandler";
            }

            @Override
            public Flowable<Map<String, Object>> run(Map<String, Object> args, SystemContext systemContext) {
                System.out.println("Handling NEEDS IMPROVEMENT");
                return Flowable.just(Map.of("action", "support", "message", "Keep trying!"));
            }
        });
        improvementHandler.setId("improvementNode");

        flowCanvas.setRoot(classifier.nextOnCondition(List.of(
            new ConditionalContainer() {
                @Override
                public Boolean eval(SystemContext systemContext) {
                    Map<String, Object> result = DelegationUtils.getResultOfNode(systemContext, "classifierNode", Map.class);
                    return "excellent".equals(result.get("category"));
                }
            }.setFlowNode(excellentHandler),
            new ConditionalContainer() {
                @Override
                public Boolean eval(SystemContext systemContext) {
                    Map<String, Object> result = DelegationUtils.getResultOfNode(systemContext, "classifierNode", Map.class);
                    return "good".equals(result.get("category"));
                }
            }.setFlowNode(goodHandler),
            new ConditionalContainer() {
                @Override
                public Boolean eval(SystemContext systemContext) {
                    Map<String, Object> result = DelegationUtils.getResultOfNode(systemContext, "classifierNode", Map.class);
                    return "needs_improvement".equals(result.get("category"));
                }
            }.setFlowNode(improvementHandler)
        )));

        System.out.println("\nTest with score = 95 (should classify as excellent)");
        Flowable<Result> flowable = new Runner().run(flowCanvas, new Request()
                .setInvokeMode(InvokeMode.SYNC)
                .setParam(Map.of("score", 95)));
        flowable.blockingIterable().forEach(event -> 
            System.out.println("Result: " + event.getData())
        );

        System.out.println("✅ Multi-branch decision completed\n");
    }
}
