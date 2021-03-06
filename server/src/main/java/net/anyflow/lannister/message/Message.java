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

package net.anyflow.lannister.message;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import net.anyflow.lannister.NettyUtil;
import net.anyflow.lannister.plugin.IMessage;
import net.anyflow.lannister.serialization.SerializableFactory;

public class Message implements com.hazelcast.nio.serialization.Portable, IMessage, Cloneable {
	public final static int ID = 1;

	public static final int MAX_MESSAGE_ID_NUM = 0xffff;
	public static final int MIN_MESSAGE_ID_NUM = 1;

	@JsonProperty
	private Integer id;
	@JsonProperty
	private String topicName;
	@JsonProperty
	private String publisherId;
	@JsonProperty
	private byte[] message;
	@JsonProperty
	private MqttQoS qos;
	@JsonProperty
	private Boolean isRetain;

	public Message() { // just for Serialization
	}

	public Message(int id, String topicName, String publisherId, byte[] message, MqttQoS qos, boolean isRetain) {
		this.id = id;
		this.topicName = topicName;
		this.publisherId = publisherId;
		this.message = message != null ? message : new byte[] {};
		this.qos = qos;
		this.isRetain = isRetain;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.lannister.message.IMessage#id()
	 */
	@Override
	public int id() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.lannister.message.IMessage#topicName()
	 */
	@Override
	public String topicName() {
		return topicName;
	}

	@Override
	public String publisherId() {
		return publisherId;
	}

	public void publisherId(String publisherId) {
		this.publisherId = publisherId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.lannister.message.IMessage#message()
	 */
	@Override
	public byte[] message() {
		return message;
	}

	public void setMessage(byte[] message) {
		this.message = message != null ? message : new byte[] {};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.lannister.message.IMessage#qos()
	 */
	@Override
	public MqttQoS qos() {
		return qos;
	}

	public void setQos(MqttQoS qos) {
		this.qos = qos;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.lannister.message.IMessage#isRetain()
	 */
	@Override
	public boolean isRetain() {
		return isRetain;
	}

	public void setRetain(boolean isRetain) {
		this.isRetain = isRetain;
	}

	@Override
	public String toString() {
		return new StringBuilder(StringUtil.simpleClassName(this)).append('[').append("id=").append(id)
				.append(", topeName=").append(topicName).append(", message=")
				.append(new String(message, CharsetUtil.UTF_8)).append(", QoS=").append(qos).append(", retain=")
				.append(isRetain).append(']').toString();
	}

	public String key() {
		return key(publisherId, id);
	}

	@Override
	public Message clone() {
		return new Message(id, topicName, publisherId, message, qos, isRetain);
	}

	public static String key(String clientId, int messageId) {
		return clientId + "_" + Integer.toString(messageId);
	}

	@JsonIgnore
	@Override
	public int getFactoryId() {
		return SerializableFactory.ID;
	}

	@JsonIgnore
	@Override
	public int getClassId() {
		return ID;
	}

	@Override
	public void writePortable(PortableWriter writer) throws IOException {
		List<String> nullChecker = Lists.newArrayList();

		if (id != null) {
			writer.writeInt("id", id);
			nullChecker.add("id");
		}

		if (topicName != null) {
			writer.writeUTF("topicName", topicName);
			nullChecker.add("topicName");
		}

		if (publisherId != null) {
			writer.writeUTF("publisherId", publisherId);
			nullChecker.add("publisherId");
		}

		if (message != null) {
			writer.writeByteArray("message", message);
			nullChecker.add("message");
		}

		if (qos != null) {
			writer.writeInt("qos", qos.value());
			nullChecker.add("qos");
		}

		if (isRetain != null) {
			writer.writeBoolean("isRetain", isRetain);
			nullChecker.add("isRetain");
		}

		writer.writeUTFArray("nullChecker", nullChecker.toArray(new String[0]));
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		List<String> nullChecker = Lists.newArrayList(reader.readUTFArray("nullChecker"));

		if (nullChecker.contains("id")) id = reader.readInt("id");
		if (nullChecker.contains("topicName")) topicName = reader.readUTF("topicName");
		if (nullChecker.contains("publisherId")) publisherId = reader.readUTF("publisherId");
		if (nullChecker.contains("message")) message = reader.readByteArray("message");
		if (nullChecker.contains("qos")) qos = MqttQoS.valueOf(reader.readInt("qos"));
		if (nullChecker.contains("isRetain")) isRetain = reader.readBoolean("isRetain");
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addIntField("id").addUTFField("topicName")
				.addUTFField("publisherId").addByteArrayField("message").addIntField("qos").addBooleanField("isRetain")
				.addUTFArrayField("nullChecker").build();
	}

	public static Message newMessage(String clientId, MqttPublishMessage published) {
		return new Message(published.variableHeader().messageId(), published.variableHeader().topicName(), clientId,
				NettyUtil.copy(published.payload()), published.fixedHeader().qosLevel(),
				published.fixedHeader().isRetain());
	}
}