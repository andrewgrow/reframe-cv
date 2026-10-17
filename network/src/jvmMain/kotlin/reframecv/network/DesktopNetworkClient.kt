package reframecv.network

import io.ktor.client.engine.cio.CIO

/** Supplies the desktop HTTP engine behind the common network configuration. */
fun createDesktopNetworkClient() = createNetworkClient(CIO.create())
