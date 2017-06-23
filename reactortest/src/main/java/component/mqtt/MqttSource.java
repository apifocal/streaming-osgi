package component.mqtt;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class MqttSource <T> implements IMqttMessageListener, Publisher<T> {

    private Subscriber<? super T> subscriber;
    private Function<byte[], T> converter;
    private MqttClient client;
    private String topic;
    private AtomicBoolean subScribed;
    
    public MqttSource(MqttClient client, String topic, Function<byte[], T> converter) {
        this.client = client;
        this.topic = topic;
        this.converter = converter;
        this.subScribed = new AtomicBoolean(false);
    }
    
    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
        subscriber.onSubscribe(new MqttSubscription());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        T payLoad = converter.apply(message.getPayload()); 
        this.subscriber.onNext(payLoad);
    }

    public class MqttSubscription implements Subscription {

        @Override
        public void request(long n) {
            try {
                if (subScribed.compareAndSet(false, true)) {
                    client.subscribe(topic, MqttSource.this);
                }
            } catch (MqttException e) {
                subscriber.onError(e);
            }
        }

        @Override
        public void cancel() {
            try {
                if (subScribed.compareAndSet(true, false)) {
                    client.unsubscribe(topic);
                }
            } catch (MqttException e) {
                subscriber.onError(e);
            }
        }

    }

}