package grouping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang.SerializationUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.MessageHelper;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.io.IOException;

import javax.xml.namespace.QName;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class GroupBy extends AbstractMediator { 

	public boolean mediate(MessageContext context) { 


       
				    ObjectMapper objectMapper = new ObjectMapper();
				    			try {
				    				
				    				MessageContext newctx= context;//	MessageHelper.cloneMessageContext(context) ;
				    				
				    				 org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) newctx)
				    			                .getAxis2MessageContext();		 
				    	JsonNode rootNode= objectMapper.readTree(JsonUtil.jsonPayloadToString(axis2MessageContext) );
				    		 
				    				 
				    	JsonNode arrayNode = rootNode.path("RequestData")
					                                    .path("Beneficiaries");
					     AtomicReference<Double> totalAmount = new AtomicReference<Double>(0.0);
					    AtomicInteger totalBeneficiary = new AtomicInteger(0);
					 
					   // JsonNode arrayNode=benificiryNode.withArray("");
					    Map<Object,  JsonNode> groupBy = StreamSupport.stream(arrayNode.spliterator(), false)
																	.map(obj -> (JsonNode) obj)
																	.collect(Collectors.groupingBy(node-> node.get("SubtransCode"),
																	                                Collectors.collectingAndThen(Collectors.toList(), transactionType->{
																		                                    Double groupTotalAmount = transactionType.stream().mapToDouble(node->  node.get("Amount").asDouble()).sum();
																		                                    int groupNumbOfBenificiary = transactionType.size();
																		                                   
																		                                    totalAmount.getAndSet( totalAmount.get()+groupTotalAmount);
																		                                    totalBeneficiary.getAndAdd(groupNumbOfBenificiary);
																		                                    ObjectNode objectNode = objectMapper.createObjectNode();
																		                                    objectNode.put("totalAmount", groupTotalAmount);
																		                                    objectNode.put("numbOfBenificiary", groupNumbOfBenificiary);
																		                                    objectNode.put("subtransCode", transactionType.get(0).get("SubtransCode").asText());
																	                                    return objectNode;
																	                                })));
					                                  //  System.out.println(groupBy);
					                                    ArrayNode  transactionTypes =  objectMapper.createArrayNode();
					                                    transactionTypes.addAll(groupBy.values());
					                                    
					                                    ObjectNode parentNode=    (ObjectNode) rootNode;
					                                    parentNode.set("TransactionType", transactionTypes);
					                                    parentNode.put("totalAmount",totalAmount.get() );
					                                    parentNode.put("totalBeneficiary",totalBeneficiary.get() );
					                                    context.setProperty("enriched_context", parentNode.toString());
					                                    
					                                    log.info(parentNode);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

        return true;
	}
}
