package simply.simply_study.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Home{

    @GetMapping({"/"})
    public String home() {
        return """
        <html>
            <head>
                <title>Simply Study API</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; margin: 40px; background-color: #f9f9f9; color: #333; }
                    h1 { color: #2C3E50; }
                    .card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); max-width: 650px; }
                    ul { list-style-type: none; padding: 0; }
                    li { margin-bottom: 12px; padding: 10px; background: #ecf0f1; border-radius: 4px; }
                    a { color: #3498db; text-decoration: none; font-weight: bold; }
                    a:hover { text-decoration: underline; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>📚 Welcome to Simply Study API!</h1>
                    <p>Explore our endpoints to manage courses, offerings, sessions, and bookings seamlessly.</p>
                    
                    <h3>🔗 API Documentation Resource Links:</h3>
                    <ul>
                        <li>🛠️ <strong>Swagger UI (Local Backend Sandbox):</strong> <a href="https://simply-study.onrender.com/swagger-ui/index.html" target="_blank">Open Swagger Dashboard</a></li>
                        <li>🚀 <strong>Postman API Documentation (Public Web):</strong> <a href="https://documenter.getpostman.com/view/39898850/2sBXwnts5z" target="_blank">Open Postman Collection Docs</a></li>
                    </ul>
                </div>
            </body>
        </html>
        """;
    }
}