package com.cisco.oss.tools;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

//@RunWith(SpringRunner.class)
//@SpringBootTest
@Slf4j
public class PacketDumperApplicationTests {

    @Test
    public void contextLoads() {
        Config config = new ConfigBuilder()
                .withMasterUrl("https://openshiftmaster.service.vci:8443")
                .withTrustCerts(true)
                .withOauthToken("xTqlW7pVnFpDyYjtS55JgKv6v7qvlHzQWJlpmZPmSao")
//                .withUsername("system")
//                .withPassword("admin")
                .build();
        KubernetesClient client = new DefaultKubernetesClient(config);

        final MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> pods = client.pods();


        pods.list().getItems().stream()
                .map(pod -> {

                    final String podIP = pod.getStatus().getPodIP();
                    final String hostname = pod.getSpec().getNodeName();
                    final List<Integer> ports = pod.getSpec().getContainers().stream()
                            .flatMap(container -> container.getPorts().stream()
                                    .map(containerPort -> {
                                        Integer port = containerPort.getHostPort();

                                        if (port == null) {
                                            port = containerPort.getContainerPort();
                                        }

                                        return port;
                                    })
                            )
                            .distinct()

                            .collect(Collectors.toList());

                    return new Container(hostname, podIP, ports);
                })
                .filter(container -> !container.getPorts().isEmpty())
                .forEach(container -> System.out.println(container.toString()));


    }

    class Container {
        private final String nodeName;
        private final String podIP;
        private final List<Integer> ports;

        public Container(String nodeName, String podIP, List<Integer> ports) {
            this.nodeName = nodeName;
            this.podIP = podIP;
            this.ports = ports;
        }

        public String getNodeName() {
            return nodeName;
        }

        public String getPodIP() {
            return podIP;
        }

        public List<Integer> getPorts() {
            return ports;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("nodeName", nodeName)
                    .append("podIP", podIP)
                    .append("ports", ports)
                    .toString();
        }
    }
}
