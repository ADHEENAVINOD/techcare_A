import os
import sqlite3
import pymysql

# Database configuration (for MySQL)
MYSQL_HOST = 'localhost'
MYSQL_USER = 'root'
MYSQL_PASSWORD = ''
MYSQL_DB = 'techcare'

db_type = 'sqlite' # Default fallback, will check mysql
conn_mysql = None

def get_connection():
    global db_type
    # Try connecting to MySQL
    try:
        conn = pymysql.connect(
            host=MYSQL_HOST,
            user=MYSQL_USER,
            password=MYSQL_PASSWORD,
            database=MYSQL_DB,
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
        db_type = 'mysql'
        return conn
    except Exception as e:
        # Fall back to SQLite
        db_path = os.path.join(os.path.dirname(__file__), 'techcare.db')
        conn = sqlite3.connect(db_path)
        conn.row_factory = sqlite3.Row
        db_type = 'sqlite'
        return conn

def execute_query(query, params=()):
    conn = get_connection()
    cursor = conn.cursor()
    try:
        if db_type == 'sqlite':
            # Convert %s placeholder to ? for SQLite
            query = query.replace('%s', '?')
            cursor.execute(query, params)
            conn.commit()
            return cursor.lastrowid
        else:
            cursor.execute(query, params)
            conn.commit()
            return cursor.lastrowid
    except Exception as e:
        print(f"Database Query Error: {e} | Query: {query}")
        raise e
    finally:
        cursor.close()
        conn.close()

def fetch_all(query, params=()):
    conn = get_connection()
    cursor = conn.cursor()
    try:
        if db_type == 'sqlite':
            query = query.replace('%s', '?')
            cursor.execute(query, params)
            rows = cursor.fetchall()
            return [dict(row) for row in rows]
        else:
            cursor.execute(query, params)
            return cursor.fetchall()
    except Exception as e:
        print(f"Database Fetch Error: {e} | Query: {query}")
        raise e
    finally:
        cursor.close()
        conn.close()

def fetch_one(query, params=()):
    conn = get_connection()
    cursor = conn.cursor()
    try:
        if db_type == 'sqlite':
            query = query.replace('%s', '?')
            cursor.execute(query, params)
            row = cursor.fetchone()
            return dict(row) if row else None
        else:
            cursor.execute(query, params)
            return cursor.fetchone()
    except Exception as e:
        print(f"Database Fetch One Error: {e} | Query: {query}")
        raise e
    finally:
        cursor.close()
        conn.close()

def init_db():
    """Initializes the database schema using the database_setup.sql file"""
    schema_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'db', 'database_setup.sql')
    if not os.path.exists(schema_path):
        print("Schema file database_setup.sql not found!")
        return

    with open(schema_path, 'r', encoding='utf-8') as f:
        sql_content = f.read()

    # Clean SQL content by removing comments first to prevent line matching bugs
    clean_lines = []
    for line in sql_content.split('\n'):
        line_stripped = line.strip()
        if not line_stripped.startswith('--') and line_stripped != '':
            clean_lines.append(line)
    sql_content_clean = '\n'.join(clean_lines)

    conn = get_connection()
    print(f"Initializing database using: {db_type.upper()}")

    if db_type == 'sqlite':
        # Clean MySQL specific syntax for SQLite compatibility
        sql_commands = sql_content_clean.split(';')
        cursor = conn.cursor()
        for cmd in sql_commands:
            cmd = cmd.strip()
            if not cmd or cmd.startswith('CREATE DATABASE') or cmd.startswith('USE'):
                continue
            # Replace MySQL specific syntax
            cmd = cmd.replace('ENGINE=InnoDB', '')
            cmd = cmd.replace('DEFAULT CHARSET=utf8mb4', '')
            cmd = cmd.replace('INT AUTO_INCREMENT PRIMARY KEY', 'INTEGER PRIMARY KEY AUTOINCREMENT')
            cmd = cmd.replace('ON DUPLICATE KEY UPDATE username=\'admin\'', '')
            # SQLite does not support multiple ON DUPLICATE statements natively in this style, let's catch insertions
            try:
                cursor.execute(cmd)
            except Exception as e:
                # If it's the admin insertion duplicate error, skip it
                if "UNIQUE constraint failed" in str(e):
                    continue
                print(f"Error executing SQLite schema line: {cmd}\nError: {e}")
        # Insert admin specifically for SQLite
        try:
            cursor.execute("INSERT OR IGNORE INTO login (lid, username, password, type) VALUES (1, 'admin', 'admin', 'admin')")
        except Exception as e:
            pass
        conn.commit()
        cursor.close()
    else:
        # MySQL connection
        cursor = conn.cursor()
        # Source SQL file execution in Python for MySQL
        commands = sql_content_clean.split(';')
        for command in commands:
            command = command.strip()
            if not command:
                continue
            try:
                cursor.execute(command)
            except Exception as e:
                print(f"Error executing MySQL schema line: {command}\nError: {e}")
    # Run dynamic alters to add starting_rate and specialties to serviceagent if they don't exist
    cursor = conn.cursor()
    try:
        cursor.execute("ALTER TABLE serviceagent ADD COLUMN starting_rate INT DEFAULT 300")
    except Exception:
        pass
    try:
        cursor.execute("ALTER TABLE serviceagent ADD COLUMN specialties VARCHAR(255) DEFAULT 'General Electronics'")
    except Exception:
        pass
    try:
        cursor.execute("ALTER TABLE assign ADD COLUMN chat_status VARCHAR(50) DEFAULT 'Active'")
    except Exception:
        pass
    conn.commit()
    cursor.close()

    conn.close()
    print("Database initialization completed.")
