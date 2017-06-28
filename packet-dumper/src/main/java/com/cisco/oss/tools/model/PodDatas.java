package com.cisco.oss.tools.model;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Yair Ogen (yaogen) on 28/06/2017.
 */
@Component
@Profile("openshift")
public class PodDatas {


    @Value("${openshift.host:openshiftmaster.service.vci}")
    private String openshiftHost;

    @Value("${openshift.port:8443}")
    private int openshiftPort;

    @Value("${openshift.token}")
    private String openshiftToken;

    private List<PodData> podDatas;


    @PostConstruct
    public void init(){
        Config config = new ConfigBuilder()
                .withMasterUrl("https://"+openshiftHost+":" + openshiftPort)
                .withTrustCerts(true)
                .withOauthToken(openshiftToken)
                .build();
        KubernetesClient client = new DefaultKubernetesClient(config);

        final MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> pods = client.pods();


        this.podDatas = pods.list().getItems().stream()
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
                .collect(Collectors.toList());

        //.forEach(posData -> System.out.println(posData.toString()));
    }
}
