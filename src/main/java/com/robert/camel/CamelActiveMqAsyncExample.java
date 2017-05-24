package com.robert.camel;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;

public class CamelActiveMqAsyncExample {
	public static final void main(String[] args) throws Exception {
		JndiContext jndiContext = new JndiContext();
		jndiContext.bind("testBean", new TestBean());
		
		CamelContext camelContext = new DefaultCamelContext(jndiContext);
		camelContext.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://localhost?broker.persistent=false"));
		try {
			camelContext.addRoutes(new RouteBuilder() {
				public void configure() {
					from("activemq:queue:start?asyncConsumer=true&concurrentConsumers=10")
							.to("bean:testBean?method=hello")
							.to("stream:out");
				}
			});
			ProducerTemplate template = camelContext.createProducerTemplate();
			camelContext.start();
			for (int i = 0; i < 5; i++) {
				template.sendBody("activemq:queue:start", "body" + i);
			}
			Thread.sleep(1000);
		} finally {
			camelContext.stop();
		}
	}
}
