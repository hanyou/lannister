/*
 * Copyright 2016 The Lannister Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.anyflow.lannister.session;

import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import net.anyflow.lannister.Hazelcast;
import net.anyflow.lannister.topic.Notification;

public class Sessions implements MessageListener<Notification> {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Sessions.class);

	private final IMap<String, Session> sessions;
	private final Map<ChannelId, String> clientIds; // KEY:channelId
	private final Map<String, ChannelHandlerContext> ctxs; // KEY:clientlId

	protected Sessions() {
		sessions = Hazelcast.INSTANCE.getMap("sessions");
		clientIds = Maps.newHashMap();
		ctxs = Maps.newHashMap();
	}

	public void put(Session session, ChannelHandlerContext ctx) {
		synchronized (this) {
			session.setConnected(true);
			sessions.put(session.clientId(), session); // [MQTT-3.1.2-4]
			clientIds.put(ctx.channel().id(), session.clientId());
			ctxs.put(session.clientId(), ctx);
		}
	}

	public void persist(Session session) {
		sessions.put(session.clientId(), session);
	}

	public ChannelHandlerContext channelHandlerContext(String clientId) {
		return ctxs.get(clientId);
	}

	public Session get(ChannelId channelId) {
		String clientId = clientIds.get(channelId);

		if (Strings.isNullOrEmpty(clientId)) { return null; }

		return sessions.get(clientId);
	}

	public Session get(String clientId) {
		return sessions.get(clientId);
	}

	public Map<String, ChannelHandlerContext> ctxs() {
		return ctxs;
	}

	protected void remove(Session session) {
		if (session == null) { return; }

		synchronized (this) {
			if (session.cleanSession()) { // [MQTT-3.1.2-5]
				sessions.remove(session.clientId());
			}

			ChannelId channelId = session.channelId();
			if (channelId == null) { return; }

			clientIds.remove(channelId);
			ctxs.remove(session.clientId());
		}
	}

	public IMap<String, Session> map() {
		return sessions;
	}

	@Override
	public void onMessage(Message<Notification> message) {
		Notification notified = message.getMessageObject();

		Session session = get(notified.clientId());
		if (session == null || !session.isConnected(true)) { return; }

		session.sendPublish(notified.topic(), notified.message());// [MQTT-3.3.1-8],[MQTT-3.3.1-9]
	}
}