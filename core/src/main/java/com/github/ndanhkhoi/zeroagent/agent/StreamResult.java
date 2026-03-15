package com.github.ndanhkhoi.zeroagent.agent;

import java.util.Map;

/**
 * Container for stream processing results.
 *
 * <p>This record encapsulates the output of processing an LLM response stream,
 * containing both the accumulated text content and the map of tool call builders
 * that were constructed during streaming.
 *
 * @param content the accumulated text content from all stream chunks
 * @param toolCalls the map of tool call builders indexed by their chunk position
 */
record StreamResult(String content, Map<Integer, ToolCallBuilder> toolCalls) {}
