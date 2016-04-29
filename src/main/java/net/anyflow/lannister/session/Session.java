package net.anyflow.lannister.session;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttMessage;
import net.anyflow.lannister.Jsonizable;
import net.anyflow.lannister.Literals;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageListener;
import net.anyflow.lannister.message.MessageSender;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicSubscription;

public class Session extends Jsonizable implements java.io.Serializable {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);

	private static final long serialVersionUID = -1800874748722060393L;

	public static Sessions NEXUS = new Sessions();

	private transient ChannelHandlerContext ctx;
	private transient Synchronizer synchronizer;
	private transient MessageSender messageSender;
	private transient MessageListener messageListener;
	private transient SessionDisposer sessionDisposer;

	@JsonProperty
	private final String clientId;
	@JsonProperty
	private final Map<String, TopicSubscription> topicSubscriptions;
	@JsonProperty
	private int currentMessageId;
	@JsonProperty
	private Message will;
	@JsonProperty
	private boolean isCleanSession;
	@JsonProperty
	private int keepAliveSeconds;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private final Date createTime;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private Date lastIncomingTime;

	public Session(ChannelHandlerContext ctx, String clientId, int keepAliveSeconds, boolean cleanSession,
			Message will) {
		this.clientId = clientId;
		this.createTime = new Date();
		this.topicSubscriptions = Maps.newHashMap();
		this.currentMessageId = 0;
		this.keepAliveSeconds = keepAliveSeconds;
		this.lastIncomingTime = new Date();
		this.isCleanSession = cleanSession;
		this.will = will; // [MQTT-3.1.2-9]

		revive(ctx);
	}

	public void revive(ChannelHandlerContext ctx) {
		this.ctx = ctx;
		this.synchronizer = new Synchronizer(this);
		this.messageSender = new MessageSender(ctx);
		this.messageListener = new MessageListener(this, synchronizer, messageSender, currentMessageId, ctx.executor());
		this.sessionDisposer = new SessionDisposer(ctx, clientId, will, messageSender);

		// TODO Do I must add listener to Repository.broadcaster?
	}

	public String clientId() {
		return clientId;
	}

	public ChannelId channelId() {
		return ctx.channel().id();
	}

	public Message will() {
		return will;
	}

	public boolean isCleanSession() {
		return isCleanSession;
	}

	public boolean isExpired() {
		if (keepAliveSeconds == 0) { return false; }

		return ((new Date()).getTime() - lastIncomingTime.getTime()) * 1000 < keepAliveSeconds;
	}

	public void setLastIncomingTime(Date lastIncomingTime) {
		this.lastIncomingTime = lastIncomingTime;
		synchronizer.execute();
	}

	public boolean isConnected() {
		return ctx != null && ctx.channel().isActive();
	}

	public ImmutableMap<String, TopicSubscription> topicSubscriptions() {
		return ImmutableMap.copyOf(topicSubscriptions);
	}

	public TopicSubscription[] matches(String topicName) {
		return topicSubscriptions.values().stream().filter(t -> t.isMatch(topicName)).toArray(TopicSubscription[]::new);
	}

	public TopicSubscription putTopicSubscription(TopicSubscription topicSubscription) {
		TopicSubscription ret = topicSubscriptions.put(topicSubscription.topicFilter(), topicSubscription); // [MQTT-3.8.4-3]
		synchronizer.execute();
		return ret;
	}

	public TopicSubscription removeTopicSubscription(final String topicFilter) {
		TopicSubscription ret = topicSubscriptions.remove(topicFilter);
		if (ret == null) { return null; }

		synchronizer.execute();

		Topic.NEXUS.removeSubscribers(topicFilter, clientId, true);

		// TODO [MQTT-3.10.4-3] If a Server deletes a Subscription It MUST
		// complete the delivery of any QoS 1 or QoS 2 messages which it has
		// started to send to the Client.

		return ret;
	}

	public ChannelFuture send(MqttMessage message) {
		return messageSender.send(message);
	}

	public void publishUnackedMessages() {
		// TODO publish Unacked Messages
	}

	public void dispose(boolean sendWill) {
		NEXUS.remove(this);
		sessionDisposer.dispose(sendWill);
	}

	public void onPublish(Topic topic, Message message) {
		messageListener.onPublish(topic, message);
	}
}