package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        AttachmentEntity::class,
        PromptEntity::class,
        BookmarkEntity::class,
        NoteEntity::class,
        BrowserHistoryEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        AIUsageEntity::class,
        ProviderSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NomaticDatabase : RoomDatabase() {
    abstract fun dao(): NomaticDao

    companion object {
        @Volatile
        private var INSTANCE: NomaticDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_updatedAt ON conversations(updatedAt)")
            }
        }

        fun getInstance(context: Context): NomaticDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NomaticDatabase::class.java,
                    "nomatic_workspace.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDefaultsIfEmpty(dao: NomaticDao) {
            val now = System.currentTimeMillis()
            val existing = dao.getProviderSettingsOnce()
            if (existing == null) {
                dao.upsertProviderSettings(ProviderSettingsEntity(updatedAt = now))
            } else {
                var updated = existing
                var changed = false
                if (existing.geminiDefaultModel == "gemini-2.5-flash" ||
                    existing.geminiDefaultModel == "gemini-2.0-flash" ||
                    existing.geminiDefaultModel.contains("1.5")
                ) {
                    updated = updated.copy(geminiDefaultModel = "gemini-3.8-flash")
                    changed = true
                }
                val sunsetOpenAi = setOf(
                    "o3-mini",
                    "meta-llama/llama-3.3-70b-instruct:free",
                    "meta-llama/llama-3.1-8b-instruct:free",
                    "deepseek/deepseek-r1:free",
                    "deepseek/deepseek-chat:free",
                    "google/gemini-2.0-flash-exp:free",
                    "qwen/qwen-2.5-coder-32b-instruct:free",
                    "mistralai/mistral-7b-instruct:free",
                    "anthropic/claude-3.5-sonnet",
                    "anthropic/claude-3.5-haiku"
                )
                if (existing.openAiDefaultModel in sunsetOpenAi) {
                    updated = updated.copy(openAiDefaultModel = "openrouter/free")
                    changed = true
                }
                if (changed) {
                    dao.upsertProviderSettings(updated.copy(updatedAt = now))
                }
            }

            if (dao.getPromptCount() == 0) {
                val samplePrompts = listOf(
                    PromptEntity(
                        id = "prompt_linux_diag",
                        title = "Linux Production Incident Triage",
                        description = "Analyze high CPU, I/O wait, open sockets, and systemd unit failures.",
                        content = "Act as a Principal Linux Systems Engineer. Given the following server symptoms or logs, provide a non-destructive diagnostic runbook using top/htop, iostat, ss, lsof, journalctl, and dmesg, followed by root-cause isolation steps:\n\n[Paste symptoms or logs]",
                        category = "Linux Administration",
                        tags = "linux,sre,systemd,diagnostics",
                        isFavorite = true,
                        createdAt = now - 3600_000L,
                        updatedAt = now - 3600_000L
                    ),
                    PromptEntity(
                        id = "prompt_android_sec",
                        title = "Android Security & Manifest Audit",
                        description = "Audit exported components, WebView settings, Keystore encryption, and Network Security Config.",
                        content = "Review the following Android architecture / Manifest / Kotlin snippet for OWASP MASVS security vulnerabilities (exported intents, WebView JS bridges, Keystore IV handling, cleartext traffic). Provide concrete remediation code:\n\n[Paste code]",
                        category = "Android Security",
                        tags = "android,security,owasp,keystore",
                        isFavorite = true,
                        createdAt = now - 3000_000L,
                        updatedAt = now - 3000_000L
                    ),
                    PromptEntity(
                        id = "prompt_aws_iam",
                        title = "AWS Least-Privilege IAM & EKS Policy",
                        description = "Generate least-privilege IAM JSON policies and Terraform modules for EKS/S3/EC2.",
                        content = "Design a strict least-privilege AWS IAM policy and corresponding Terraform resource block for the following workload requirement. Include resource ARNs, condition keys, and security explanations:\n\n[Describe workload]",
                        category = "AWS & Cloud",
                        tags = "aws,iam,terraform,eks",
                        isFavorite = true,
                        createdAt = now - 2400_000L,
                        updatedAt = now - 2400_000L
                    ),
                    PromptEntity(
                        id = "prompt_abinitio_eme",
                        title = "Ab Initio EME Sandbox & Tag Migration",
                        description = "Troubleshoot air commands, sandbox inheritance, psets, and EME tag promotion.",
                        content = "As an Ab Initio Administrator, explain the exact `air` CLI workflow to inspect project inheritance, resolve lock conflicts, create an EME release tag, and migrate a sandbox across DEV/QA/PROD environments safely for:\n\n[Describe graph/sandbox]",
                        category = "Ab Initio",
                        tags = "abinitio,eme,gde,air",
                        isFavorite = false,
                        createdAt = now - 1800_000L,
                        updatedAt = now - 1800_000L
                    ),
                    PromptEntity(
                        id = "prompt_bash_hardening",
                        title = "Idempotent Bash Automation Script",
                        description = "Generate POSIX/Bash scripts with strict error trapping, dry-run flags, and logging.",
                        content = "Write a production-grade Bash script using `set -euo pipefail`, trap cleanup handlers, `--dry-run` mode, and structured logging to accomplish:\n\n[Describe task]",
                        category = "Scripting",
                        tags = "bash,shell,automation,devops",
                        isFavorite = false,
                        createdAt = now - 1200_000L,
                        updatedAt = now - 1200_000L
                    ),
                    PromptEntity(
                        id = "prompt_app_analysis",
                        title = "Architecture & Code Review Synthesis",
                        description = "Evaluate concurrency, memory safety, Clean Architecture boundaries, and edge cases.",
                        content = "Perform a senior architecture and code review on the following implementation. Flag race conditions, main-thread blocking, leak risks, and error handling gaps:\n\n[Paste code or spec]",
                        category = "Application Analysis",
                        tags = "architecture,kotlin,review",
                        isFavorite = true,
                        createdAt = now - 600_000L,
                        updatedAt = now - 600_000L
                    )
                )
                dao.upsertPrompts(samplePrompts)
            }

            if (dao.getNoteCount() == 0) {
                val sampleNotes = listOf(
                    NoteEntity(
                        id = "note_linux_quickref",
                        title = "Linux Incident Command Reference",
                        content = "# Linux Incident Triage\n\n```bash\n# Check listening sockets & PIDs\nss -tulnp\n\n# Inspect top disk consumers\ndu -xh /var/log | sort -rh | head -n 15\n\n# Follow systemd service logs\njournalctl -u nginx.service -f --since \"15 minutes ago\"\n```",
                        category = "Linux commands",
                        tags = "PINNED,linux,sre,sockets",
                        createdAt = now - 7200_000L,
                        updatedAt = now - 7200_000L
                    ),
                    NoteEntity(
                        id = "note_abinitio_air",
                        title = "Ab Initio EME & Sandbox Runbook",
                        content = "# Ab Initio EME Administration\n\n- **Check object status**: `air object versions <eme-path>`\n- **Create tag**: `air tag create <tag-name> -file <pset-or-mp>`\n- **Export tag save file**: `air save <save-file.sav> -from-tag <tag-name>`\n- **Check locks**: `air lock show -project <project-path>`",
                        category = "Technical documentation",
                        tags = "PINNED,abinitio,eme,air",
                        createdAt = now - 5400_000L,
                        updatedAt = now - 5400_000L
                    ),
                    NoteEntity(
                        id = "note_ldap_filters",
                        title = "LDAPS & Active Directory Search Filters",
                        content = "# LDAP & AD Query Cheatsheet\n\n```bash\nldapsearch -H ldaps://ldap.corp.example.com:636 \\\n  -D \"CN=svc_bind,OU=ServiceAccounts,DC=corp,DC=example,DC=com\" \\\n  -W -b \"DC=corp,DC=example,DC=com\" \\\n  \"(&(objectClass=user)(sAMAccountName=jdoe))\" cn mail memberOf\n```",
                        category = "Scripts",
                        tags = "ldap,activedirectory,security",
                        createdAt = now - 3600_000L,
                        updatedAt = now - 3600_000L
                    )
                )
                dao.upsertNotes(sampleNotes)
            }

            if (dao.getTaskCount() == 0) {
                val sampleTasks = listOf(
                    TaskEntity(
                        id = UUID.randomUUID().toString(),
                        title = "Configure OpenAI or Gemini API Key in AI Provider Management",
                        description = "Encrypted locally with Android Keystore AES-256-GCM",
                        dueAt = now + 86_400_000L,
                        priority = "HIGH",
                        isCompleted = false,
                        createdAt = now - 1800_000L
                    ),
                    TaskEntity(
                        id = UUID.randomUUID().toString(),
                        title = "Audit EME sandbox tag promotion script before Q4 release",
                        description = "Verify air tag create -exact flags in Developer Assistant",
                        dueAt = now + 172_800_000L,
                        priority = "MEDIUM",
                        isCompleted = false,
                        createdAt = now - 1200_000L
                    ),
                    TaskEntity(
                        id = UUID.randomUUID().toString(),
                        title = "Kubernetes CKS Security & NetworkPolicy Deep Dive",
                        description = "Track progress in Learning Tracker tab",
                        dueAt = null,
                        priority = "LEARNING",
                        isCompleted = false,
                        createdAt = now - 900_000L
                    )
                )
                dao.upsertTasks(sampleTasks)
            }
        }
    }
}
