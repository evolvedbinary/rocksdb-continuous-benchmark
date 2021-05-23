package com.evolvedbinary.rocksdb.cb.github;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Optional;

public class Server {
    private final Settings settings;
    private final WebHookPayloadSummaryProcessor webHookPayloadSummaryProcessor;

    public Server(final Settings settings, final WebHookPayloadSummaryProcessor webHookPayloadSummaryProcessor) {
        this.settings = settings;
        this.webHookPayloadSummaryProcessor = webHookPayloadSummaryProcessor;
    }

    public void runSync() throws InterruptedException {
        final Instance instance = runAsync();
        instance.awaitShutdown();
    }

    public Instance runAsync() throws InterruptedException {
        // Load the certificates and initiate the SSL Context
        final SSLHandlerProvider sslHandlerProvider = new SSLHandlerProvider(settings.keystore, settings.keystorePassword, settings.certificatePassword);
        sslHandlerProvider.init();

        // Configure the bootstrap.
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            final ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    //.childHandler(new ServerHandlersInit())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(final SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    sslHandlerProvider.getSSLHandler(),
                                    new HttpServerCodec(),
                                    new HttpObjectAggregator(1048576),
                                    new RequestHandler(webHookPayloadSummaryProcessor)
                            );
                        }})
                    .childOption(ChannelOption.AUTO_READ, true);

            // Bind and start to accept incoming connections.
            final ChannelFuture f = b.bind(settings.port).sync();

            return new Instance(f.channel().closeFuture(), bossGroup, workerGroup);

        } catch (final RuntimeException | InterruptedException | Error rt) {
            if (rt instanceof InterruptedException) {
                Thread.currentThread().interrupt();  // restore interrupt flag
            }
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw rt;
        }
    }

    static class Instance implements Closeable {
        private final ChannelFuture closeFuture;
        private final EventLoopGroup bossGroup;
        private final EventLoopGroup workerGroup;

        private Instance(final ChannelFuture closeFuture, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
            this.closeFuture = closeFuture;
            this.bossGroup = bossGroup;
            this.workerGroup = workerGroup;
        }

        /**
         * Wait until the server socket is closed.
         */
        public void awaitShutdown() throws InterruptedException {
            try {
                this.closeFuture.sync();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }

        @Override
        public void close() {
            try {
                this.closeFuture.channel().close();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }
    }

    static class Settings {
        final Path keystore;
        final Optional<String> keystorePassword;
        final Optional<String> certificatePassword;
        final int port;

        public Settings(final Path keystore, final Optional<String> keystorePassword, final Optional<String> certificatePassword, final int port) {
            this.keystore = keystore;
            this.keystorePassword = keystorePassword;
            this.certificatePassword = certificatePassword;
            this.port = port;
        }
    }
}
