package com.example.core.utilities

enum class SafetyRiskLevel {
    CRITICAL,
    WARNING,
    SAFE
}

data class HazardDetail(
    val title: String,
    val level: SafetyRiskLevel,
    val explanation: String,
    val safeAlternative: String
)

data class CommandSafetyReport(
    val isDestructive: Boolean,
    val highestRiskLevel: SafetyRiskLevel,
    val matchedHazards: List<HazardDetail>,
    val safetyGuidance: String
)

object DeveloperSafetyAnalyzer {

    private val hazardRules = listOf(
        Triple(
            Regex("\\brm\\s+-[rfRF]+"),
            HazardDetail(
                title = "Recursive/Forced File Deletion (rm -rf)",
                level = SafetyRiskLevel.CRITICAL,
                explanation = "Permanently deletes target directory trees without confirmation prompt.",
                safeAlternative = "Use interactive deletion 'rm -i' or staging directory move 'mv <target> /tmp/' first."
            ),
            true
        ),
        Triple(
            Regex(":\\(\\)\\{\\s*:\\|:&\\s*\\};:"),
            HazardDetail(
                title = "Shell Fork Bomb",
                level = SafetyRiskLevel.CRITICAL,
                explanation = "Recursively spawns processes until system resources and PID space are exhausted.",
                safeAlternative = "Do NOT run. Limit process bounds via ulimit -u if testing scripts."
            ),
            true
        ),
        Triple(
            Regex("\\bmkfs(\\.[a-z0-9]+)?\\b"),
            HazardDetail(
                title = "Filesystem Formatting (mkfs)",
                level = SafetyRiskLevel.CRITICAL,
                explanation = "Wipes partition block allocation tables and destroys existing filesystems.",
                safeAlternative = "Verify drive identifier via 'lsblk -f' before formatting non-production volumes."
            ),
            true
        ),
        Triple(
            Regex("\\bdd\\s+if="),
            HazardDetail(
                title = "Raw Device Block Overwrite (dd)",
                level = SafetyRiskLevel.CRITICAL,
                explanation = "Direct block write can overwrite master boot records or active volume data.",
                safeAlternative = "Check target path 'of=' with 'fdisk -l' and specify status=progress with backup."
            ),
            true
        ),
        Triple(
            Regex("\\bchmod\\s+(-R\\s+)?777\\b"),
            HazardDetail(
                title = "World-Writable Permission Escalation (chmod 777)",
                level = SafetyRiskLevel.WARNING,
                explanation = "Grants read, write, and execute permissions to all local users and web processes.",
                safeAlternative = "Use least-privilege mode 'chmod 755' or 'chmod 644' for standard files."
            ),
            true
        ),
        Triple(
            Regex("\\b(shutdown|reboot|init\\s+0|poweroff)\\b"),
            HazardDetail(
                title = "System Power State Modification",
                level = SafetyRiskLevel.WARNING,
                explanation = "Initiates immediate server shutdown or reboot, terminating active sessions.",
                safeAlternative = "Schedule maintenance window via 'shutdown +15 \"Server Maintenance\"' first."
            ),
            true
        ),
        Triple(
            Regex("\\b(DROP\\s+TABLE|DROP\\s+DATABASE|TRUNCATE\\s+TABLE)\\b", RegexOption.IGNORE_CASE),
            HazardDetail(
                title = "Destructive SQL DDL Operation",
                level = SafetyRiskLevel.CRITICAL,
                explanation = "Removes relational database schemas or purges all stored records instantly.",
                safeAlternative = "Perform mysqldump / pg_dump backup and run inside explicit transaction blocks."
            ),
            true
        ),
        Triple(
            Regex("\\bterraform\\s+destroy\\b"),
            HazardDetail(
                title = "Infrastructure Tear-down (terraform destroy)",
                level = SafetyRiskLevel.CRITICAL,
                explanation = "Deprovisions cloud compute, network, and storage assets defined in tfstate.",
                safeAlternative = "Generate execution plan first using 'terraform plan -destroy'."
            ),
            true
        ),
        Triple(
            Regex("\\bkubectl\\s+delete\\s+(namespace|ns|node|pv|pvc|--all)\\b"),
            HazardDetail(
                title = "Kubernetes Cluster Resource Deletion",
                level = SafetyRiskLevel.WARNING,
                explanation = "Terminates cluster namespaces or persistent volumes in active context.",
                safeAlternative = "Verify current cluster context via 'kubectl config current-context' and use '--dry-run=client'."
            ),
            true
        ),
        Triple(
            Regex("\\baws\\s+s3\\s+(rb|rm)\\b.*--force"),
            HazardDetail(
                title = "Forced AWS S3 Object / Bucket Deletion",
                level = SafetyRiskLevel.WARNING,
                explanation = "Purges cloud object storage without confirmation prompt.",
                safeAlternative = "Test S3 file operations using 'aws s3 rm ... --dryrun'."
            ),
            true
        ),
        Triple(
            Regex("\\b(air\\s+object\\s+rm|air\\s+tag\\s+delete|m_rm)\\b"),
            HazardDetail(
                title = "Ab Initio EME / Multifile Object Removal",
                level = SafetyRiskLevel.WARNING,
                explanation = "Deletes Enterprise Meta Environment objects or multifiles permanently.",
                safeAlternative = "Check EME branch tag via 'air tag list' before executing object deletion."
            ),
            true
        ),
        Triple(
            Regex("\\bgit\\s+push\\s+.*--force\\b"),
            HazardDetail(
                title = "Force Git Repository Push (git push --force)",
                level = SafetyRiskLevel.WARNING,
                explanation = "Overwrites remote branch commit history and discards remote commits.",
                safeAlternative = "Use safer force-with-lease option 'git push --force-with-lease'."
            ),
            true
        )
    )

    fun inspectContent(text: String): CommandSafetyReport {
        val matches = hazardRules.mapNotNull { (regex, detail, _) ->
            if (regex.containsMatchIn(text)) detail else null
        }

        if (matches.isEmpty()) {
            return CommandSafetyReport(
                isDestructive = false,
                highestRiskLevel = SafetyRiskLevel.SAFE,
                matchedHazards = emptyList(),
                safetyGuidance = "Terminal Guard: Command snippet verified. No destructive patterns detected."
            )
        }

        val highest = when {
            matches.any { it.level == SafetyRiskLevel.CRITICAL } -> SafetyRiskLevel.CRITICAL
            else -> SafetyRiskLevel.WARNING
        }

        val guidance = if (highest == SafetyRiskLevel.CRITICAL) {
            "CRITICAL RISK DETECTED: Contains destructive commands (${matches.joinToString { it.title }}). Verify parameters, backups, and target hosts before running."
        } else {
            "WARNING: Contains elevated risk operations (${matches.joinToString { it.title }}). Ensure dry-run verification."
        }

        return CommandSafetyReport(
            isDestructive = true,
            highestRiskLevel = highest,
            matchedHazards = matches,
            safetyGuidance = guidance
        )
    }
}
