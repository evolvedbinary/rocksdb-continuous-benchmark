package com.evolvedbinary.rocksdb.cb.github;

import com.evolvedbinary.rocksdb.cb.dataobject.WebHookPayloadSummary;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final WebHookPayloadSummaryProcessor requestBodyProcessor;

    public RequestHandler(final WebHookPayloadSummaryProcessor requestBodyProcessor) {
        this.requestBodyProcessor = requestBodyProcessor;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) {

        final String uri = request.uri();

        // ONLY accept POST requests
        if (request.method().equals(HttpMethod.POST)) {

            // ONLY match the URI "/cb"
            if ("/cb".equals(uri)) {

                // ONLY match the Content Type "application/json"
                final String contentType = request.headers().get("Content-Type");
                if ("application/json".equals(contentType)) {

                    final ByteBuf requestContentBuf = request.content();
                    try {
                        final String content = requestContentBuf.toString(UTF_8);

                        // Reject empty body
                        if (content != null && !content.isEmpty()) {

                            try {
                                final WebHookPayloadSummary webHookPayloadSummary = new WebHookPayloadParser().parse(content);

                                // Send the content to the request body processor
                                requestBodyProcessor.process(webHookPayloadSummary);

                                // 202 ACCEPTED
                                sendResponse(ctx, HttpResponseStatus.ACCEPTED);

                            } catch (final WebHookPayloadParser.InvalidJsonException e) {
                                // 400 BAD REQUEST
                                sendExceptionResponse(ctx, HttpResponseStatus.BAD_REQUEST, e);

                            } catch (final WebHookPayloadParser.InvalidPayloadException e) {
                                // 422 UNPROCESSABLE ENTITY
                                sendExceptionResponse(ctx, HttpResponseStatus.UNPROCESSABLE_ENTITY, e);

                            } catch (final IOException e) {
                                // 503 SERVICE UNAVAILABLE
                                sendExceptionResponse(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, e);
                            }

                        } else {
                            // 400 BAD REQUEST
                            sendResponse(ctx, HttpResponseStatus.BAD_REQUEST);
                        }

                    } finally {
                        //byteBuf.release();
                    }

                } else {
                    // 415 UNSUPPORTED MEDIA TYPE
                    sendResponse(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                }
            } else {
                // 404 NOT FOUND
                sendResponse(ctx, HttpResponseStatus.NOT_FOUND);
            }

        } else {
            // 405 METHOD NOT ALLOWED
            sendResponse(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
        }

        ctx.channel().close();

    }

    private static void sendExceptionResponse(final ChannelHandlerContext ctx, final HttpResponseStatus httpResponseStatus, final Exception e) {
        if (e.getMessage() != null) {
            ByteBuf responseContentBuf = null;
            try {
                responseContentBuf = Unpooled.wrappedBuffer(e.getMessage().getBytes(UTF_8));
                ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, httpResponseStatus, responseContentBuf));
            } finally {
//                if (responseContentBuf != null) {
//                    responseContentBuf.release();
//                }
            }
        } else {
            ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, httpResponseStatus));
        }
    }

    private static void sendResponse(final ChannelHandlerContext ctx, final HttpResponseStatus httpResponseStatus) {
        ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, httpResponseStatus));
    }
}
