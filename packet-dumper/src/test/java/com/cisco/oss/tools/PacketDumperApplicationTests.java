package com.cisco.oss.tools;

import com.cisco.oss.tools.model.PodData;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
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
                .withNamespace("ivp")
//                .withOauthToken("zmb1kysjcwk_R9S5HniUg3hVPk8fk4wz1SlECIUBFM8")
//                kubectl config view | grep token | awk '{print $2}'
                .withUsername("system")
                .withPassword("admin")
                .build();
        KubernetesClient client = new DefaultOpenShiftClient(config);


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

                    return new PodData(hostname, podIP, ports);
                })
                .filter(podData -> !podData.getPorts().isEmpty())
                .forEach(posData -> System.out.println(posData.toString()));


    }


}
