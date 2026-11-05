# Features

The base CardDemo user journeys run through CICS terminal screens and host batch jobs. [CardDemo features](core/index.md) covers access, accounts, cards, transactions, billing, and batch outputs. Menu definitions are in `app/cpy/COMEN02Y.cpy` and `app/cpy/COADM02Y.cpy`.

Optional authorization viewing and Db2 reference-data administration extend those journeys, but require additional installed products and resources. Their internal behavior is documented under [Systems](../systems/index.md), not duplicated here.

See [Architecture](../overview/architecture.md) for shared runtime boundaries and [Testing](../how-to-contribute/testing.md) before changing financial processing.
