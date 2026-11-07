# Business archetype of a Kickstarter-like crowdfunding platform

Source of archetype definitions: Neal Cabage, *Business Model Archetypes*, https://nealcabage.com/business-model-archetypes/

## Answer

A Kickstarter-like crowdfunding platform is a **Marketplace**. The platform itself is the product: a self-service website where creators list projects and backers find and fund them. It earns a commission on each successfully funded project (Kickstarter takes 5%). It also has the network effect Cabage describes: more backers attract more creators, which attract more backers.

**Assumed scope:** rewards-based crowdfunding. The platform takes no ownership of the projects, doesn't guarantee that rewards are delivered, and charges creators a percentage of the money raised. Backers aren't strictly investing: they get rewards, not shares, so this works more like a preorder.

## How I tested each archetype

Cabage defines each archetype by two things: a **key activity** (what the business does to create value) and a **way of earning money** (what the customer pays for). For each archetype I asked:

1. What do creators and backers actually get from the platform?
2. Does the platform's main work match the archetype's key activity?
3. Does the way the platform earns money match the archetype's way of earning money?

## Analysis per archetype

| # | Archetype | Cabage's definition | Analysis | Result |
|---|---|---|---|---|
| 1 | **Product** | One-time purchase of an artifact; key activity is product development | The platform is software, but nobody buys it. Creators and backers use it for free, and money is made only when a project is funded. Product is one ingredient of the Marketplace, not the business model. | **NO** |
| 2 | **Service** | Doing something for the customer and charging a fee; paid for time and materials | The platform doesn't do the creators' work or get paid for its time. Project review and support are minor and aren't how it earns money. | **NO** |
| 3 | **Trade** | Connecting buyers and sellers; earns by buying low and selling high; key activities are sourcing and advertising | It connects backers with creators, but never buys the rewards to resell them, so there's no resale margin. Trade is the other ingredient of the Marketplace, not the business model on its own. | **NO** |
| 4 | **Subscription** | Product + Service with ongoing access for a recurring payment | There's no recurring fee for access. Each pledge is a one-off payment. (A Patreon-style platform would be closer to this.) | **NO** |
| 5 | **Brokerage** | Trade + Service: trading on behalf of clients for a base fee plus commission | The platform doesn't find or arrange funding on a creator's behalf. Creators list and promote their projects themselves, and the platform takes no responsibility for the deal. | **NO** |
| 6 | **Marketplace** | Trade + Product: a self-service platform that recruits vendors and takes a commission per sale | Direct match. Its main work is attracting creators (the sellers) and backers (the buyers), and it takes a commission on each funded project. Its value grows with the size of the network. | **YES** |
| 7 | **Ecosystem** | Product + Service + Trade together, with partners and platforms | There's no Service element and no partner or platform layer. Adding paid campaign management, fulfillment or a store for finished products could move it toward an ecosystem. | **NO** |

## The judgment calls

- **Marketplace or Brokerage?** The deciding question is who does the deal-making. On Kickstarter, creators and backers deal with each other directly and the platform only takes its commission. A firm that actively finds backers for clients and charges a fee plus commission would be a broker. That's also closer to equity crowdfunding, if "invest" means real shares rather than rewards.
- **Marketplace or Trade?** If the platform bought the rewards upfront and resold them to backers at a markup, it would be a Trade business. It doesn't.
