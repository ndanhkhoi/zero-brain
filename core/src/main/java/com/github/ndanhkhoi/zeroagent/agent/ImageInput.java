package com.github.ndanhkhoi.zeroagent.agent;

import java.io.InputStream;

/**
 * A wrapper record for providing visual files to the LLM agent via InputStream.
 *
 * @param stream The raw byte stream of the image file.
 * @param mimeType The appropriate MIME type of the file (e.g., "image/png", "image/jpeg").
 */
public record ImageInput(InputStream stream, String mimeType) {}
