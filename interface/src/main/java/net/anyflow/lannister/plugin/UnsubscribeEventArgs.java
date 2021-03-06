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

package net.anyflow.lannister.plugin;

import java.util.List;

public interface UnsubscribeEventArgs {
	String clientId();

	List<String> topicFilters();

	default String log() {
		if (topicFilters() == null) { return null; }

		StringBuilder sb = new StringBuilder();
		sb.append("clientId=").append(clientId());

		topicFilters().stream().forEach(t -> sb.append(", topicFilter=").append(t));

		return sb.toString();
	}
}
