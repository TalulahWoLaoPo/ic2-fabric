package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

class ManualKineticGeneratorSync(schema: SyncSchema) {
    var storedKu by schema.int("StoredKu")
    var extractedKu by schema.int("ExtractedKu")
}