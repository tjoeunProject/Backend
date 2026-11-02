# Token column deployment check

G3 tokens include purpose and UUID claims; allow at least 1024 characters. This is a review template, not an executed migration.

1. Confirm actual Oracle schema owner/table name and inspect USER_TAB_COLUMNS for TOKEN.TOKEN DATA_TYPE, CHAR_LENGTH and CHAR_USED.
2. Back up according to the project's migration process; verify no other deployment is writing an incompatible schema.
3. Review the additive expansion for the actual owner: `ALTER TABLE TOKEN MODIFY (TOKEN VARCHAR2(1024 CHAR));`.
4. Deploy Backend #56, Python #58 and Frontend #74 with matching JWT/Hashids/proxy settings. Existing untyped sessions must re-login.
5. Verify register/login/refresh/logout/deletion against actual Oracle. No production SQL has been run by this task.

Rollback must not blindly shrink a column containing >255-character tokens. Stop/revoke affected sessions and inspect lengths first; review rollback with the actual schema and deployment order. H2 tests do not validate this Oracle DDL.
