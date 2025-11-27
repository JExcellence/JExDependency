# Translation Bundles

Translation files load in stage 3 when
[`RDQ#initializeRepositories()`](../../java/com/raindropcentral/rdq/RDQ.java) connects the shared
i18n repository. Translation hydration uses the stage-1 executor (virtual threads with fixed-pool
fallback) to prepare message catalogs off the main thread before views render them inside the
[`runSync`](../../java/com/raindropcentral/rdq/RDQ.java) boundary.

Both editions consume the same bundles through the shared view frame. Coordinate updates with the
RDQ manager and bounty service Javadocs to avoid divergences between free and premium UX.

RDQ offers the following languages. Other languages supported by adding custom yml file.

| Code      | Language              | Country/Region       | Description                          |
| --------- | --------------------- | -------------------- | ------------------------------------ |
| **ar_EG** | Arabic                | Egypt                | Egyptian Arabic                      |
| **ar_SD** | Arabic                | Sudan                | Sudanese Arabic                      |
| **ar_XA** | Arabic                | Generic (Arab world) | Generic or pan-Arabic locale         |
| **bn_BD** | Bengali               | Bangladesh           | Bangla (Bangladesh)                  |
| **cs_CZ** | Czech                 | Czech Republic       | Standard Czech                       |
| **da_DK** | Danish                | Denmark              | Danish (Denmark)                     |
| **de_DE** | German                | Germany              | Standard German                      |
| **el_GR** | Greek                 | Greece               | Modern Greek                         |
| **en_GB** | English               | United Kingdom       | British English                      |
| **en_US** | English               | United States        | American English                     |
| **es_ES** | Spanish               | Spain                | European Spanish                     |
| **fa_IR** | Persian (Farsi)       | Iran                 | Iranian Persian                      |
| **fi_FI** | Finnish               | Finland              | Finnish (Finland)                    |
| **fr_FR** | French                | France               | Standard French                      |
| **hi_IN** | Hindi                 | India                | Hindi (India)                        |
| **id_ID** | Indonesian            | Indonesia            | Bahasa Indonesia                     |
| **it_IT** | Italian               | Italy                | Standard Italian                     |
| **ja_JP** | Japanese              | Japan                | Japanese                             |
| **ko_KR** | Korean                | South Korea          | Korean                               |
| **pl_PL** | Polish                | Poland               | Polish                               |
| **pt_BR** | Portuguese            | Brazil               | Brazilian Portuguese                 |
| **pt_PT** | Portuguese            | Portugal             | European Portuguese                  |
| **ru_RU** | Russian               | Russia               | Russian                              |
| **sv_SE** | Swedish               | Sweden               | Swedish                              |
| **th_TH** | Thai                  | Thailand             | Thai                                 |
| **tl_PH** | Tagalog               | Philippines          | Filipino/Tagalog                     |
| **tr_TR** | Turkish               | Turkey               | Turkish                              |
| **vi_VN** | Vietnamese            | Vietnam              | Vietnamese                           |
| **zh_CN** | Chinese (Simplified)  | China                | Simplified Mandarin (Mainland China) |
| **zh_TW** | Chinese (Traditional) | Taiwan               | Traditional Mandarin (Taiwan)        |
