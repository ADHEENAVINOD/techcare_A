import os
from datetime import datetime
from flask import Flask, render_template, request, redirect, url_for, session, flash, jsonify
from werkzeug.utils import secure_filename
import db_helper

app = Flask(__name__)
app.secret_key = 'techcare_bachelor_project_secret_key'

# Configure upload folder for photos (products and complaints)
UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), 'static', 'uploads')
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# Initialize database tables on startup
db_helper.init_db()

# --- WEB DASHBOARD ROUTES ---

@app.route('/')
def index():
    if 'lid' in session:
        if session['type'] == 'admin':
            return redirect(url_for('admin_dashboard'))
        elif session['type'] == 'company':
            return redirect(url_for('company_dashboard'))
    return redirect(url_for('login'))

@app.route('/login', methods=['GET'])
def login():
    return render_template('login.html')

@app.route('/login_post', methods=['POST'])
def login_post():
    username = request.form['username']
    password = request.form['password']

    user_info = db_helper.fetch_one(
        "SELECT * FROM login WHERE username = %s AND password = %s",
        (username, password)
    )

    if user_info:
        # Check if company is approved before letting them log in
        if user_info['type'] == 'company':
            company_info = db_helper.fetch_one(
                "SELECT status FROM company WHERE co_lid = %s",
                (user_info['lid'],)
            )
            if company_info and company_info['status'] != 'Approved':
                flash("Your company registration is pending approval by the Admin.", "error")
                return redirect(url_for('login'))

        session['lid'] = user_info['lid']
        session['username'] = user_info['username']
        session['type'] = user_info['type']
        flash("Logged in successfully!", "success")
        
        if user_info['type'] == 'admin':
            return redirect(url_for('admin_dashboard'))
        elif user_info['type'] == 'company':
            return redirect(url_for('company_dashboard'))
        else:
            session.clear()
            flash("Unauthorized role type.", "error")
            return redirect(url_for('login'))
    else:
        flash("Invalid username or password.", "error")
        return redirect(url_for('login'))

@app.route('/logout')
def logout():
    session.clear()
    flash("Logged out successfully.", "success")
    return redirect(url_for('login'))

@app.route('/register_company', methods=['GET'])
def register_company():
    return render_template('register_company.html')

@app.route('/register_company_post', methods=['POST'])
def register_company_post():
    name = request.form['name']
    email = request.form['email']
    phone = request.form['phone']
    house = request.form['house']
    place = request.form['place']
    district = request.form['district']
    pin_code = request.form['pin_code']
    license = request.form['license']
    username = request.form['username']
    password = request.form['password']

    # Check if username already exists
    existing = db_helper.fetch_one("SELECT lid FROM login WHERE username = %s", (username,))
    if existing:
        flash("Username already taken. Please choose another one.", "error")
        return redirect(url_for('register_company'))

    try:
        # 1. Insert into Login
        lid = db_helper.execute_query(
            "INSERT INTO login (username, password, type) VALUES (%s, %s, 'company')",
            (username, password)
        )
        # 2. Insert into Company
        db_helper.execute_query(
            "INSERT INTO company (co_lid, name, email, phone, house, district, pin_code, license, status) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, 'Pending')",
            (lid, name, email, phone, house, district, pin_code, license)
        )
        flash("Registration successful! Waiting for admin approval.", "success")
        return redirect(url_for('login'))
    except Exception as e:
        flash(f"Error registering company: {e}", "error")
        return redirect(url_for('register_company'))

# Admin Dashboard
@app.route('/admin_dashboard')
def admin_dashboard():
    if 'lid' not in session or session['type'] != 'admin':
        return redirect(url_for('login'))

    tab = request.args.get('tab', 'home')
    
    # Fetch data based on active tab
    companies = []
    agents = []
    users = []
    stats = {}

    if tab == 'home':
        c_count = db_helper.fetch_one("SELECT COUNT(*) as cnt FROM company")['cnt']
        a_count = db_helper.fetch_one("SELECT COUNT(*) as cnt FROM serviceagent")['cnt']
        u_count = db_helper.fetch_one("SELECT COUNT(*) as cnt FROM user")['cnt']
        stats = {
            'companies_count': c_count,
            'agents_count': a_count,
            'users_count': u_count
        }
    elif tab == 'companies':
        companies = db_helper.fetch_all("SELECT * FROM company")
    elif tab == 'technicians':
        agents = db_helper.fetch_all(
            "SELECT sa.*, c.name as company_name FROM serviceagent sa JOIN company c ON sa.company_id = c.co_lid"
        )
    elif tab == 'users':
        users = db_helper.fetch_all("SELECT * FROM user")

    return render_template('admin_dashboard.html', tab=tab, companies=companies, agents=agents, users=users, stats=stats)

@app.route('/admin_approve_company/<int:co_id>')
def admin_approve_company(co_id):
    if 'lid' not in session or session['type'] != 'admin':
        return redirect(url_for('login'))

    db_helper.execute_query(
        "UPDATE company SET status = 'Approved' WHERE co_id = %s",
        (co_id,)
    )
    flash("Company registration approved successfully!", "success")
    return redirect(url_for('admin_dashboard', tab='companies'))

# Company Dashboard
@app.route('/company_dashboard')
def company_dashboard():
    if 'lid' not in session or session['type'] != 'company':
        return redirect(url_for('login'))

    co_lid = session['lid']
    tab = request.args.get('tab', 'home')

    profile = db_helper.fetch_one("SELECT * FROM company WHERE co_lid = %s", (co_lid,))
    products = []
    agents = []
    complaints = []
    ratings = []
    parts_requests = []
    stats = {}

    if tab == 'home':
        p_count = db_helper.fetch_one("SELECT COUNT(*) as cnt FROM product WHERE com_lid = %s", (co_lid,))['cnt']
        a_count = db_helper.fetch_one("SELECT COUNT(*) as cnt FROM serviceagent WHERE company_id = %s", (co_lid,))['cnt']
        c_count = db_helper.fetch_one(
            "SELECT COUNT(*) as cnt FROM complaint c JOIN product p ON c.pid = p.product_id WHERE p.com_lid = %s",
            (co_lid,)
        )['cnt']
        stats = {
            'products_count': p_count,
            'agents_count': a_count,
            'complaints_count': c_count
        }
    elif tab == 'products':
        products = db_helper.fetch_all("SELECT * FROM product WHERE com_lid = %s", (co_lid,))
    elif tab == 'agents':
        agents = db_helper.fetch_all("SELECT * FROM serviceagent WHERE company_id = %s", (co_lid,))
    elif tab == 'complaints':
        agents = db_helper.fetch_all("SELECT * FROM serviceagent WHERE company_id = %s", (co_lid,))
        complaints_raw = db_helper.fetch_all(
            """SELECT c.*, p.product_name, p.amount as product_amount, u.name as user_name, u.phone as user_phone
               FROM complaint c 
               JOIN product p ON c.pid = p.product_id 
               JOIN user u ON c.user_id = u.user_lid
               WHERE p.com_lid = %s""",
            (co_lid,)
        )
        # Fetch technician assignment names
        complaints = []
        for item in complaints_raw:
            assigned = db_helper.fetch_one(
                "SELECT sa.name FROM assign a JOIN serviceagent sa ON a.tid = sa.sagent_lid WHERE a.cid = %s",
                (item['cid'],)
            )
            item['agent_name'] = assigned['name'] if assigned else None
            complaints.append(item)
    elif tab == 'ratings':
        ratings = db_helper.fetch_all(
            "SELECT r.*, u.name as user_name FROM rating r JOIN user u ON r.user_lid = u.user_lid WHERE r.service_lid = %s",
            (co_lid,)
        )
    elif tab == 'parts':
        parts_requests = db_helper.fetch_all(
            """SELECT pr.*, p.product_name, p.amount, c.complaint, u.name as user_name
               FROM parts_request pr
               JOIN product p ON pr.product_id = p.product_id
               JOIN complaint c ON pr.cid = c.cid
               JOIN user u ON c.user_id = u.user_lid
               WHERE p.com_lid = %s
               ORDER BY pr.request_id DESC""",
            (co_lid,)
        )

    return render_template(
        'company_dashboard.html',
        tab=tab,
        profile=profile,
        products=products,
        agents=agents,
        complaints=complaints,
        ratings=ratings,
        parts_requests=parts_requests,
        stats=stats
    )

@app.route('/company_update_part_status_web', methods=['POST'])
def company_update_part_status_web():
    if 'lid' not in session or session['type'] != 'company':
        return redirect(url_for('login'))
    request_id = request.form.get('request_id')
    status = request.form.get('status', 'Sourced and Dispatched')
    
    db_helper.execute_query(
        "UPDATE parts_request SET status = %s WHERE request_id = %s",
        (status, request_id)
    )
    flash("Genuine component sourced and dispatched to technician successfully!", "success")
    return redirect(url_for('company_dashboard', tab='parts'))

@app.route('/company_profile_update', methods=['POST'])
def company_profile_update():
    if 'lid' not in session or session['type'] != 'company':
        return redirect(url_for('login'))

    co_lid = session['lid']
    name = request.form['name']
    email = request.form['email']
    phone = request.form['phone']
    house = request.form['house']
    district = request.form['district']
    pin_code = request.form['pin_code']

    db_helper.execute_query(
        """UPDATE company 
           SET name = %s, email = %s, phone = %s, house = %s, district = %s, pin_code = %s 
           WHERE co_lid = %s""",
        (name, email, phone, house, district, pin_code, co_lid)
    )
    flash("Profile updated successfully!", "success")
    return redirect(url_for('company_dashboard', tab='profile'))

@app.route('/company_add_product', methods=['POST'])
def company_add_product():
    if 'lid' not in session or session['type'] != 'company':
        return redirect(url_for('login'))

    co_lid = session['lid']
    product_name = request.form['product_name']
    amount = request.form['amount']
    description = request.form['description']
    filename = None

    # Handle image upload
    if 'photo' in request.files:
        file = request.files['photo']
        if file and file.filename != '' and allowed_file(file.filename):
            filename = secure_filename(f"prod_{int(datetime.now().timestamp())}_{file.filename}")
            file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))

    db_helper.execute_query(
        "INSERT INTO product (product_name, com_lid, amount, photo, description) VALUES (%s, %s, %s, %s, %s)",
        (product_name, co_lid, amount, filename, description)
    )
    flash("Product added successfully!", "success")
    return redirect(url_for('company_dashboard', tab='products'))

@app.route('/company_delete_product/<int:product_id>')
def company_delete_product(product_id):
    if 'lid' not in session or session['type'] != 'company':
        return redirect(url_for('login'))

    # Optionally delete file from static folder
    product = db_helper.fetch_one("SELECT photo FROM product WHERE product_id = %s", (product_id,))
    if product and product['photo']:
        try:
            os.remove(os.path.join(app.config['UPLOAD_FOLDER'], product['photo']))
        except Exception:
            pass

    db_helper.execute_query("DELETE FROM product WHERE product_id = %s", (product_id,))
    flash("Product deleted successfully!", "success")
    return redirect(url_for('company_dashboard', tab='products'))

@app.route('/company_add_agent', methods=['POST'])
def company_add_agent():
    if 'lid' not in session or session['type'] != 'company':
        return redirect(url_for('login'))

    co_lid = session['lid']
    name = request.form['agent_name']
    email = request.form['agent_email']
    phone = request.form['agent_phone']
    place = request.form['agent_place']
    starting_rate = request.form.get('starting_rate', 300)
    specialties = request.form.get('specialties', 'General Electronics')
    username = request.form['agent_username']
    password = request.form['agent_password']

    try:
        starting_rate = int(request.form.get('starting_rate', 300))
    except ValueError:
        starting_rate = 300

    if starting_rate > 300:
        flash("Technician starting visit rate cannot exceed ₹300 for customer protection.", "error")
        return redirect(url_for('company_dashboard', tab='agents'))

    # Check if username already exists
    existing = db_helper.fetch_one("SELECT lid FROM login WHERE username = %s", (username,))
    if existing:
        flash("Username already taken. Please choose another one.", "error")
        return redirect(url_for('company_dashboard', tab='agents'))

    try:
        # 1. Insert login
        lid = db_helper.execute_query(
            "INSERT INTO login (username, password, type) VALUES (%s, %s, 'technician')",
            (username, password)
        )
        # 2. Insert service agent
        db_helper.execute_query(
            "INSERT INTO serviceagent (sagent_lid, company_id, name, email, phone, place, starting_rate, specialties) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)",
            (lid, co_lid, name, email, phone, place, starting_rate, specialties)
        )
        flash("Service agent added successfully!", "success")
    except Exception as e:
        flash(f"Error adding service agent: {e}", "error")
        
    return redirect(url_for('company_dashboard', tab='agents'))

@app.route('/company_delete_agent/<int:sa_id>')
def company_delete_agent(sa_id):
    if 'lid' not in session or session['type'] != 'company':
        return redirect(url_for('login'))

    agent = db_helper.fetch_one("SELECT sagent_lid FROM serviceagent WHERE sa_id = %s", (sa_id,))
    if agent:
        db_helper.execute_query("DELETE FROM login WHERE lid = %s", (agent['sagent_lid'],))
        flash("Service agent removed successfully!", "success")
    else:
        flash("Service agent not found.", "error")
        
    return redirect(url_for('company_dashboard', tab='agents'))

@app.route('/company_send_estimate/<int:cid>', methods=['POST'])
def company_send_estimate(cid):
    if 'lid' not in session or session['type'] != 'company':
        return redirect(url_for('login'))

    amount = request.form['amount']
    db_helper.execute_query(
        "UPDATE complaint SET estimate_amount = %s, status = 'Estimated' WHERE cid = %s",
        (amount, cid)
    )
    flash("Estimate sent to the customer successfully!", "success")
    return redirect(url_for('company_dashboard', tab='complaints'))

@app.route('/company_assign_technician/<int:cid>', methods=['POST'])
def company_assign_technician(cid):
    if 'lid' not in session or session['type'] != 'company':
        return redirect(url_for('login'))

    tid = request.form['tid'] # login ID of service agent
    
    # Create assignment
    db_helper.execute_query(
        "INSERT INTO assign (cid, tid, remark, status) VALUES (%s, %s, 'Assigned to work', 'Assigned')",
        (cid, tid)
    )
    # Update complaint status
    db_helper.execute_query(
        "UPDATE complaint SET status = 'Assigned' WHERE cid = %s",
        (cid,)
    )
    flash("Technician assigned successfully!", "success")
    return redirect(url_for('company_dashboard', tab='complaints'))

@app.route('/change_password_post', methods=['POST'])
def change_password_post():
    if 'lid' not in session:
        return redirect(url_for('login'))

    old_password = request.form['old_password']
    new_password = request.form['new_password']
    confirm_password = request.form['confirm_password']

    if new_password != confirm_password:
        flash("New passwords do not match.", "error")
        return redirect(url_for('index'))

    user = db_helper.fetch_one(
        "SELECT password FROM login WHERE lid = %s", (session['lid'],)
    )

    if user and user['password'] == old_password:
        db_helper.execute_query(
            "UPDATE login SET password = %s WHERE lid = %s",
            (new_password, session['lid'])
        )
        flash("Password changed successfully!", "success")
    else:
        flash("Incorrect old password.", "error")

    # Redirect based on role
    if session['type'] == 'admin':
        return redirect(url_for('admin_dashboard', tab='password'))
    else:
        return redirect(url_for('company_dashboard', tab='password'))


# --- CLIENT ANDROID API ENDPOINTS ---

@app.route('/api/login', methods=['POST'])
def api_login():
    data = request.get_json() or request.form
    username = data.get('username')
    password = data.get('password')

    user = db_helper.fetch_one(
        "SELECT * FROM login WHERE username = %s AND password = %s",
        (username, password)
    )

    if user:
        # Check approval for company
        if user['type'] == 'company':
            company = db_helper.fetch_one("SELECT status FROM company WHERE co_lid = %s", (user['lid'],))
            if not company or company['status'] != 'Approved':
                return jsonify({'status': 'failed', 'message': 'Company pending approval'}), 401
        
        # Load associated profile name
        name = "Administrator"
        profile_id = 0
        if user['type'] == 'user':
            profile = db_helper.fetch_one("SELECT name, user_id FROM user WHERE user_lid = %s", (user['lid'],))
            if profile:
                name = profile['name']
                profile_id = profile['user_id']
        elif user['type'] == 'technician':
            profile = db_helper.fetch_one("SELECT name, sa_id FROM serviceagent WHERE sagent_lid = %s", (user['lid'],))
            if profile:
                name = profile['name']
                profile_id = profile['sa_id']
        elif user['type'] == 'company':
            profile = db_helper.fetch_one("SELECT name, co_id FROM company WHERE co_lid = %s", (user['lid'],))
            if profile:
                name = profile['name']
                profile_id = profile['co_id']

        return jsonify({
            'status': 'success',
            'lid': user['lid'],
            'username': user['username'],
            'type': user['type'],
            'name': name,
            'profile_id': profile_id
        })
    else:
        return jsonify({'status': 'failed', 'message': 'Invalid login details'}), 401

@app.route('/api/user_register', methods=['POST'])
def api_user_register():
    name = request.form.get('name')
    email = request.form.get('email')
    phone = request.form.get('phone')
    house = request.form.get('house')
    place = request.form.get('place')
    city = request.form.get('city')
    state = request.form.get('state')
    pin = request.form.get('pin')
    username = request.form.get('username')
    password = request.form.get('password')
    filename = None

    # Handle optional photo upload
    if 'photo' in request.files:
        file = request.files['photo']
        if file and file.filename != '' and allowed_file(file.filename):
            filename = secure_filename(f"user_{int(datetime.now().timestamp())}_{file.filename}")
            file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))

    existing = db_helper.fetch_one("SELECT lid FROM login WHERE username = %s", (username,))
    if existing:
        return jsonify({'status': 'failed', 'message': 'Username already exists'}), 400

    try:
        lid = db_helper.execute_query(
            "INSERT INTO login (username, password, type) VALUES (%s, %s, 'user')",
            (username, password)
        )
        db_helper.execute_query(
            """INSERT INTO user (user_lid, name, photo, house, place, city, state, pin, email, phone) 
               VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
            (lid, name, filename, house, place, city, state, pin, email, phone)
        )
        return jsonify({'status': 'success', 'message': 'User registered successfully'})
    except Exception as e:
        return jsonify({'status': 'failed', 'message': str(e)}), 500

@app.route('/api/view_companies', methods=['GET'])
def api_view_companies():
    companies = db_helper.fetch_all("SELECT * FROM company WHERE status = 'Approved'")
    
    # Calculate star ratings dynamically for each company
    processed = []
    for c in companies:
        rating_info = db_helper.fetch_one(
            "SELECT AVG(CAST(rating AS DECIMAL(10,2))) as avg_r FROM rating WHERE service_lid = %s",
            (c['co_lid'],)
        )
        c['avg_rating'] = str(round(rating_info['avg_r'], 1)) if rating_info and rating_info['avg_r'] else '0.0'
        processed.append(c)

    return jsonify({'status': 'success', 'data': processed})

@app.route('/api/view_products', methods=['GET'])
def api_view_products():
    co_lid = request.args.get('co_lid')
    products = db_helper.fetch_all("SELECT * FROM product WHERE com_lid = %s", (co_lid,))
    return jsonify({'status': 'success', 'data': products})

@app.route('/api/send_complaint', methods=['POST'])
def api_send_complaint():
    pid = request.form.get('pid')
    complaint_text = request.form.get('complaint')
    user_lid = request.form.get('user_lid')
    date_str = datetime.now().strftime("%Y-%m-%d")
    filename = None

    if 'photo' in request.files:
        file = request.files['photo']
        if file and file.filename != '' and allowed_file(file.filename):
            filename = secure_filename(f"comp_{int(datetime.now().timestamp())}_{file.filename}")
            file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))

    try:
        db_helper.execute_query(
            "INSERT INTO complaint (complaint, pid, date, cphoto, status, estimate_amount, user_id) VALUES (%s, %s, %s, %s, 'Pending', 'Pending', %s)",
            (complaint_text, pid, date_str, filename, user_lid)
        )
        return jsonify({'status': 'success', 'message': 'Complaint submitted successfully'})
    except Exception as e:
        return jsonify({'status': 'failed', 'message': str(e)}), 500

@app.route('/api/view_complaints', methods=['GET'])
def api_view_complaints():
    user_lid = request.args.get('user_lid')
    complaints = db_helper.fetch_all(
        """SELECT c.*, p.product_name, p.com_lid, com.name as company_name, com.phone as company_phone
           FROM complaint c
           JOIN product p ON c.pid = p.product_id
           JOIN company com ON p.com_lid = com.co_lid
           WHERE c.user_id = %s""",
        (user_lid,)
    )

    # Attach technician profile details to each complaint if assigned
    data = []
    for item in complaints:
        assigned = db_helper.fetch_one(
            """SELECT sa.name, sa.phone, sa.place, sa.sagent_lid as tid 
               FROM assign a 
               JOIN serviceagent sa ON a.tid = sa.sagent_lid 
               WHERE a.cid = %s""",
            (item['cid'],)
        )
        if assigned:
            item['agent_name'] = assigned['name']
            item['agent_phone'] = assigned['phone']
            item['agent_place'] = assigned['place']
            item['tid'] = assigned['tid']
        else:
            item['agent_name'] = None
            item['agent_phone'] = None
            item['agent_place'] = None
            item['tid'] = None
        data.append(item)

    return jsonify({'status': 'success', 'data': data})

@app.route('/api/approve_estimate', methods=['POST'])
def api_approve_estimate():
    data = request.get_json() or request.form
    cid = data.get('cid')
    status = data.get('status') # 'Approved' or 'Rejected'

    db_helper.execute_query(
        "UPDATE complaint SET status = %s WHERE cid = %s",
        (status, cid)
    )
    return jsonify({'status': 'success', 'message': f'Estimate {status.lower()} successfully'})

@app.route('/api/give_rating', methods=['POST'])
def api_give_rating():
    data = request.get_json() or request.form
    user_lid = data.get('user_lid')
    service_lid = data.get('service_lid') # login ID of the company
    stars = data.get('rating')
    date_str = datetime.now().strftime("%Y-%m-%d")

    db_helper.execute_query(
        "INSERT INTO rating (user_lid, service_lid, rating, date) VALUES (%s, %s, %s, %s)",
        (user_lid, service_lid, stars, date_str)
    )
    return jsonify({'status': 'success', 'message': 'Rating submitted successfully'})

@app.route('/api/send_feedback', methods=['POST'])
def api_send_feedback():
    data = request.get_json() or request.form
    user_lid = data.get('user_lid')
    sagent_id = data.get('tech_lid') # login ID of technician
    feedback_text = data.get('feedback')
    date_str = datetime.now().strftime("%Y-%m-%d")

    db_helper.execute_query(
        "INSERT INTO feedback (sagent_id, user_lid, feedback, date) VALUES (%s, %s, %s, %s)",
        (sagent_id, user_lid, feedback_text, date_str)
    )
    return jsonify({'status': 'success', 'message': 'Feedback submitted successfully'})

# --- Technician REST APIs ---

@app.route('/api/technician/view_assigned', methods=['GET'])
def api_tech_view_assigned():
    tech_lid = request.args.get('tech_lid')
    assigned_jobs = db_helper.fetch_all(
        """SELECT c.*, p.product_name, p.com_lid as company_id, u.name as user_name, u.phone as user_phone, u.house as user_house, u.place as user_place, a.aid
           FROM assign a
           JOIN complaint c ON a.cid = c.cid
           JOIN product p ON c.pid = p.product_id
           JOIN user u ON c.user_id = u.user_lid
           WHERE a.tid = %s""",
        (tech_lid,)
    )
    return jsonify({'status': 'success', 'data': assigned_jobs})

@app.route('/api/technician/update_status', methods=['POST'])
def api_tech_update_status():
    data = request.get_json() or request.form
    cid = data.get('cid')
    status = data.get('status') # e.g. 'In Progress', 'Completed'

    # Update both complaint and assign status
    db_helper.execute_query(
        "UPDATE complaint SET status = %s WHERE cid = %s",
        (status, cid)
    )
    db_helper.execute_query(
        "UPDATE assign SET status = %s WHERE cid = %s",
        (status, cid)
    )
    return jsonify({'status': 'success', 'message': 'Job status updated'})

@app.route('/api/technician/view_feedback', methods=['GET'])
def api_tech_view_feedback():
    tech_lid = request.args.get('tech_lid')
    feedbacks = db_helper.fetch_all(
        "SELECT f.*, u.name as user_name FROM feedback f JOIN user u ON f.user_lid = u.user_lid WHERE f.sagent_id = %s",
        (tech_lid,)
    )
    return jsonify({'status': 'success', 'data': feedbacks})

# --- Live Chat API ---

@app.route('/api/chat/send', methods=['POST'])
def api_chat_send():
    data = request.get_json(silent=True) or request.form
    from_id = data.get('from_id')
    to_id = data.get('to_id')
    message = data.get('message')
    date_str = datetime.now().strftime("%Y-%m-%d")
    time_str = datetime.now().strftime("%H:%M:%S")

    # Check if chat is blocked
    blocked = db_helper.fetch_one(
        """SELECT aid FROM assign a
           JOIN complaint c ON a.cid = c.cid
           WHERE ((a.tid = %s AND c.user_id = %s) OR (a.tid = %s AND c.user_id = %s))
           AND a.chat_status = 'Blocked'""",
        (from_id, to_id, to_id, from_id)
    )
    if blocked:
        return jsonify({'status': 'blocked', 'message': 'This chat has been closed/blocked.'})

    db_helper.execute_query(
        "INSERT INTO live_chat (from_id, to_id, date, time, message) VALUES (%s, %s, %s, %s, %s)",
        (from_id, to_id, date_str, time_str, message)
    )
    return jsonify({'status': 'success', 'message': 'Message sent'})

@app.route('/api/chat/get', methods=['GET'])
def api_chat_get():
    from_id = request.args.get('from_id')
    to_id = request.args.get('to_id')
    
    blocked = db_helper.fetch_one(
        """SELECT aid FROM assign a
           JOIN complaint c ON a.cid = c.cid
           WHERE ((a.tid = %s AND c.user_id = %s) OR (a.tid = %s AND c.user_id = %s))
           AND a.chat_status = 'Blocked'""",
        (from_id, to_id, to_id, from_id)
    )

    chats = db_helper.fetch_all(
        """SELECT * FROM live_chat 
           WHERE (from_id = %s AND to_id = %s) OR (from_id = %s AND to_id = %s)
           ORDER BY chat_id ASC""",
        (from_id, to_id, to_id, from_id)
    )
    return jsonify({
        'status': 'success', 
        'data': chats, 
        'blocked': True if blocked else False
    })

# --- Parts Sourcing APIs (Authentic Sourcing Workflow) ---

@app.route('/api/technician/request_part', methods=['POST'])
def api_tech_request_part():
    data = request.get_json(silent=True) or request.form
    cid = data.get('cid')
    product_id = data.get('product_id')
    
    db_helper.execute_query(
        "INSERT INTO parts_request (cid, product_id, status) VALUES (%s, %s, 'Pending Sourcing')",
        (cid, product_id)
    )
    return jsonify({'status': 'success', 'message': 'Genuine part sourcing requested from manufacturer'})

@app.route('/api/company/parts_requests', methods=['GET'])
def api_company_parts_requests():
    co_lid = request.args.get('co_lid')
    requests_data = db_helper.fetch_all(
        """SELECT pr.*, p.product_name, p.amount, c.complaint, u.name as user_name
           FROM parts_request pr
           JOIN product p ON pr.product_id = p.product_id
           JOIN complaint c ON pr.cid = c.cid
           JOIN user u ON c.user_id = u.user_lid
           WHERE p.com_lid = %s
           ORDER BY pr.request_id DESC""",
        (co_lid,)
    )
    return jsonify({'status': 'success', 'data': requests_data})

@app.route('/api/company/update_part_status', methods=['POST'])
def api_company_update_part_status():
    request_id = request.form.get('request_id')
    status = request.form.get('status')
    
    db_helper.execute_query(
        "UPDATE parts_request SET status = %s WHERE request_id = %s",
        (status, request_id)
    )
    return jsonify({'status': 'success', 'message': 'Part dispatch status updated'})

# --- Technician Info & Receipt Sourcing APIs ---

@app.route('/api/view_technicians', methods=['GET'])
def api_view_technicians():
    company_id = request.args.get('company_id')
    techs = db_helper.fetch_all(
        """SELECT sa.sa_id, sa.sagent_lid, sa.company_id, sa.name, sa.place, sa.starting_rate, sa.specialties, l.username
           FROM serviceagent sa
           JOIN login l ON sa.sagent_lid = l.lid
           WHERE sa.company_id = %s""",
        (company_id,)
    )
    for t in techs:
        # Fetch feedbacks specifically for this technician
        feedbacks = db_helper.fetch_all(
            "SELECT f.*, u.name as user_name FROM feedback f JOIN user u ON f.user_lid = u.user_lid WHERE f.sagent_id = %s",
            (t['sagent_lid'],)
        )
        if feedbacks:
            t['avg_rating'] = sum(float(f['rating']) for f in feedbacks) / len(feedbacks)
            t['reviews'] = feedbacks
        else:
            t['avg_rating'] = 0.0
            t['reviews'] = []
    return jsonify({'status': 'success', 'data': techs})

@app.route('/api/view_receipt', methods=['GET'])
def api_view_receipt():
    cid = request.args.get('cid')
    job = db_helper.fetch_one(
        """SELECT c.*, p.product_name, sa.name as tech_name, sa.starting_rate, sa.specialties
           FROM complaint c
           JOIN product p ON c.pid = p.product_id
           LEFT JOIN assign a ON c.cid = a.cid
           LEFT JOIN serviceagent sa ON a.tid = sa.sagent_lid
           WHERE c.cid = %s""",
        (cid,)
    )
    if not job:
        return jsonify({'status': 'failed', 'message': 'Complaint not found'}), 404
        
    parts = db_helper.fetch_all(
        """SELECT pr.*, p.product_name, p.amount
           FROM parts_request pr
           JOIN product p ON pr.product_id = p.product_id
           WHERE pr.cid = %s AND pr.status = 'Sourced and Dispatched'""",
        (cid,)
    )
    
    base_rate = job['starting_rate'] if (job.get('starting_rate') is not None) else 300
    parts_total = sum(float(p['amount']) for p in parts)
    total_amount = base_rate + parts_total
    
    return jsonify({
        'status': 'success',
        'tech_name': job['tech_name'] or 'Not Assigned',
        'starting_rate': base_rate,
        'specialties': job['specialties'] or 'General Electronics',
        'parts': parts,
        'parts_total': parts_total,
        'total_amount': total_amount,
        'complaint': job['complaint'],
        'product_name': job['product_name']
    })


# --- Chat Blocking & Stop Conversation APIs ---

@app.route('/api/chat/block', methods=['POST'])
def api_chat_block():
    data = request.get_json(silent=True) or request.form
    cid = data.get('cid')
    db_helper.execute_query(
        "UPDATE assign SET chat_status = 'Blocked' WHERE cid = %s",
        (cid,)
    )
    return jsonify({'status': 'success', 'message': 'Conversation closed/blocked.'})

# --- Dynamic Job Bidding & Matching APIs ---

@app.route('/api/technician/available_jobs', methods=['GET'])
def api_technician_available_jobs():
    tech_lid = request.args.get('tech_lid')
    tech = db_helper.fetch_one("SELECT company_id FROM serviceagent WHERE sagent_lid = %s", (tech_lid,))
    if not tech:
        return jsonify({'status': 'failed', 'message': 'Technician not found'}), 404
    
    co_lid = tech['company_id']
    
    # Select complaints for this company's products that are Pending or Estimated
    # and exclude complaints that this technician has already offered to do
    jobs = db_helper.fetch_all(
        """SELECT c.*, p.product_name, p.amount as product_price, u.name as user_name, u.place as user_place
           FROM complaint c
           JOIN product p ON c.pid = p.product_id
           JOIN user u ON c.user_id = u.user_lid
           WHERE p.com_lid = %s AND c.status IN ('Pending', 'Estimated', 'Approved')
           AND c.cid NOT IN (SELECT cid FROM assign WHERE tid = %s)
           ORDER BY c.cid DESC""",
        (co_lid, tech_lid)
    )
    return jsonify({'status': 'success', 'data': jobs})

@app.route('/api/technician/offer_job', methods=['POST'])
def api_technician_offer_job():
    data = request.get_json(silent=True) or request.form
    cid = data.get('cid')
    tech_lid = data.get('tech_lid')
    
    # Check if already offered
    existing = db_helper.fetch_one("SELECT aid FROM assign WHERE cid = %s AND tid = %s", (cid, tech_lid))
    if existing:
        db_helper.execute_query(
            "UPDATE assign SET status = 'Offered', chat_status = 'Active' WHERE cid = %s AND tid = %s",
            (cid, tech_lid)
        )
    else:
        db_helper.execute_query(
            "INSERT INTO assign (cid, tid, status, chat_status) VALUES (%s, %s, 'Offered', 'Active')",
            (cid, tech_lid)
        )
    return jsonify({'status': 'success', 'message': 'Offer sent to customer.'})

@app.route('/api/user/complaint_offers', methods=['GET'])
def api_user_complaint_offers():
    cid = request.args.get('cid')
    offers = db_helper.fetch_all(
        """SELECT a.aid, a.status as offer_status, sa.*, l.username
           FROM assign a
           JOIN serviceagent sa ON a.tid = sa.sagent_lid
           JOIN login l ON sa.sagent_lid = l.lid
           WHERE a.cid = %s AND a.status IN ('Offered', 'Accepted', 'Assigned')""",
        (cid,)
    )
    
    data = []
    for o in offers:
        rating_info = db_helper.fetch_one(
            "SELECT AVG(CAST(rating AS FLOAT)) as avg_rating FROM rating WHERE service_lid = %s",
            (o['sagent_lid'],)
        )
        feedbacks = db_helper.fetch_all(
            "SELECT f.*, u.name as user_name FROM feedback f JOIN user u ON f.user_lid = u.user_lid WHERE f.sagent_id = %s",
            (o['sagent_lid'],)
        )
        avg_val = rating_info['avg_rating'] if (rating_info and rating_info['avg_rating']) else 0.0
        
        comments = []
        for f in feedbacks:
            comments.append({
                'feedback': f['feedback'],
                'date': f['date'],
                'user_name': f['user_name']
            })
            
        o['avg_rating'] = round(avg_val, 1)
        o['reviews'] = comments
        o.pop('phone', None)
        o.pop('email', None)
        data.append(o)
        
    return jsonify({'status': 'success', 'data': data})

@app.route('/api/user/accept_offer', methods=['POST'])
def api_user_accept_offer():
    data = request.get_json(silent=True) or request.form
    aid = data.get('aid')
    cid = data.get('cid')
    
    db_helper.execute_query("UPDATE assign SET status = 'Accepted' WHERE aid = %s", (aid,))
    db_helper.execute_query("UPDATE assign SET status = 'Rejected' WHERE cid = %s AND aid != %s", (cid, aid))
    db_helper.execute_query("UPDATE complaint SET status = 'Accepted' WHERE cid = %s", (cid,))
    
    return jsonify({'status': 'success', 'message': 'Offer accepted. Waiting for technician confirmation.'})

@app.route('/api/technician/confirm_job', methods=['POST'])
def api_technician_confirm_job():
    data = request.get_json(silent=True) or request.form
    cid = data.get('cid')
    tech_lid = data.get('tech_lid')
    
    db_helper.execute_query(
        "UPDATE assign SET status = 'Assigned' WHERE cid = %s AND tid = %s",
        (cid, tech_lid)
    )
    db_helper.execute_query(
        "UPDATE complaint SET status = 'Assigned' WHERE cid = %s",
        (cid,)
    )
    return jsonify({'status': 'success', 'message': 'Job started successfully!'})

@app.route('/api/technician/cancel_job', methods=['POST'])
def api_technician_cancel_job():
    data = request.get_json(silent=True) or request.form
    cid = data.get('cid')
    tech_lid = data.get('tech_lid')
    
    db_helper.execute_query(
        "UPDATE assign SET status = 'Cancelled' WHERE cid = %s AND tid = %s",
        (cid, tech_lid)
    )
    db_helper.execute_query(
        "UPDATE complaint SET status = 'Pending' WHERE cid = %s",
        (cid,)
    )
    return jsonify({'status': 'success', 'message': 'Job connection cancelled.'})

if __name__ == '__main__':
    # Listen on all local interfaces on port 5000
    app.run(host='0.0.0.0', port=5000, debug=True)
