package com.robert.camel;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;

public class TopicDisableReplyToExample {
	public static final void main(String[] args) throws Exception {
		JndiContext jndiContext = new JndiContext();
		jndiContext.bind("testBean", new TestBean());

		CamelContext camelContext = new DefaultCamelContext(jndiContext);
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
		camelContext.addComponent("activemq", jmsComponentAutoAcknowledge(connectionFactory));
		try {
			camelContext.addRoutes(new RouteBuilder() {
				public void configure() {
					from("activemq:queue:myQueue")
                    .transform(body().prepend("XYZ "))
                    .to("log:queueResult")
                    .to("direct:queueResult");

                    from("activemq:topic:myTopic?disableReplyTo=true")
                    .to("log:topicResult")
                    .to("direct:topicResult");
                    
                    from("direct:queueResult")
                    .transform(simple("direct:queueResult output: ${body}"))
                    .to("stream:out");
                    
                    from("direct:topicResult")
                    .transform(simple("direct:topicResult output: ${body}"))
                    .to("stream:out");
				}
			});
			ProducerTemplate template = camelContext.createProducerTemplate();
			camelContext.start();
			template.send("activemq:queue:myQueue?preserveMessageQos=true", new Processor() {
	            public void process(Exchange exchange) throws Exception {
	                exchange.getIn().setBody("ABC");
	                exchange.getIn().setHeader("JMSReplyTo", "topic:myTopic");
	            }
	        });
			Thread.sleep(1000);
		} finally {
			camelContext.stop();
		}
	}
	
    public static JmsComponent jmsComponentAutoAcknowledge(ConnectionFactory connectionFactory) {
        JmsConfiguration template = new JmsConfiguration(connectionFactory);
        template.setAcknowledgementMode(Session.AUTO_ACKNOWLEDGE);
        return new JmsComponent(template);
    }
}
