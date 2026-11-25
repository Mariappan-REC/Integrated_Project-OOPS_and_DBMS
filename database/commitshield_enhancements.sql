-- =====================================================================
-- CommitShield 2.0: Database Enhancements
-- Database: commitshield
-- Purpose: Add task_dependencies and task_history without modifying
--          or recreating the existing `tasks` table.
-- =====================================================================

USE commitshield;

-- ---------------------------------------------------------------------
-- Table 1: task_dependencies
-- Records prerequisite relationships between tasks.
-- e.g., Task B (task_id) depends on Task A (depends_on_task_id).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_dependencies (
    task_id INT NOT NULL,
    depends_on_task_id INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Composite primary key prevents duplicate dependency pairs
    PRIMARY KEY (task_id, depends_on_task_id),
    
    -- Prevents direct self-dependency (a task cannot depend on itself)
    CONSTRAINT chk_task_not_self_dependent 
        CHECK (task_id <> depends_on_task_id),
        
    -- Foreign Key: references dependent task
    -- Deletion behavior: When task is deleted, only its dependency links are removed.
    -- Unrelated tasks are never deleted.
    CONSTRAINT fk_task_dep_task 
        FOREIGN KEY (task_id) 
        REFERENCES tasks(id) 
        ON DELETE CASCADE,
        
    -- Foreign Key: references prerequisite task
    -- Deletion behavior: When prerequisite task is deleted, prerequisite links are removed.
    CONSTRAINT fk_task_dep_prereq 
        FOREIGN KEY (depends_on_task_id) 
        REFERENCES tasks(id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- Table 2: task_history
-- Records genuine status transitions for tasks (audit trail).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    old_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Prevents recording duplicate status without an actual change
    CONSTRAINT chk_status_changed 
        CHECK (old_status <> new_status),
        
    -- Foreign Key: references task
    -- Deletion behavior: When a task is deleted, its audit history rows
    -- are cleaned up with it, preserving referential integrity.
    CONSTRAINT fk_task_history_task 
        FOREIGN KEY (task_id) 
        REFERENCES tasks(id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- InnoDB is a MySQL storage engine that supports transactions and foreign keys.
-- utf8mb4 supports Unicode text, including a wide range of special characters and emojis.
