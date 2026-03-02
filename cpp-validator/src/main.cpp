#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>

#include <cstdlib>
#include <cstring>
#include <iostream>
#include <regex>
#include <sstream>
#include <string>
#include <thread>

#include <openssl/err.h>
#include <openssl/ssl.h>

std::string get_env(const char* key, const std::string& fallback) {
    const char* value = std::getenv(key);
    return value ? value : fallback;
}

double extract_amount(const std::string& json) {
    std::regex amount_regex("\\\"amount\\\"\\s*:\\s*([0-9]+(\\.[0-9]+)?)");
    std::smatch match;
    if (std::regex_search(json, match, amount_regex)) {
        return std::stod(match[1]);
    }
    return 0.0;
}

std::string evaluate(const std::string& validator_id, const std::string& payload) {
    double amount = extract_amount(payload);
    bool approved = amount <= 10000.0;
    std::string reason = approved ? "within validator threshold" : "amount exceeds threshold";
    std::ostringstream out;
    out << "{\"validatorId\":\"" << validator_id << "\",\"decision\":\""
        << (approved ? "APPROVE" : "REJECT") << "\",\"reason\":\"" << reason << "\"}";
    return out.str();
}

void handle_client(SSL* ssl, const std::string& validator_id) {
    char buffer[4096];
    int read_bytes = SSL_read(ssl, buffer, sizeof(buffer) - 1);
    if (read_bytes <= 0) {
        SSL_shutdown(ssl);
        SSL_free(ssl);
        return;
    }
    buffer[read_bytes] = '\0';
    std::string payload(buffer);
    std::string response = evaluate(validator_id, payload) + "\n";
    SSL_write(ssl, response.c_str(), static_cast<int>(response.size()));
    SSL_shutdown(ssl);
    SSL_free(ssl);
}

int main() {
    const std::string validator_id = get_env("VALIDATOR_ID", "validator-1");
    const int port = std::stoi(get_env("VALIDATOR_PORT", "9443"));
    const std::string cert = get_env("TLS_CERT", "./certs/validator1.pem");
    const std::string key = get_env("TLS_KEY", "./certs/validator1.key");
    const std::string ca = get_env("TLS_CA", "./certs/ca.pem");

    SSL_library_init();
    OpenSSL_add_all_algorithms();
    SSL_load_error_strings();

    const SSL_METHOD* method = TLS_server_method();
    SSL_CTX* ctx = SSL_CTX_new(method);
    if (!ctx) {
        ERR_print_errors_fp(stderr);
        return 1;
    }

    SSL_CTX_set_min_proto_version(ctx, TLS1_2_VERSION);

    if (SSL_CTX_use_certificate_file(ctx, cert.c_str(), SSL_FILETYPE_PEM) <= 0 ||
        SSL_CTX_use_PrivateKey_file(ctx, key.c_str(), SSL_FILETYPE_PEM) <= 0 ||
        SSL_CTX_load_verify_locations(ctx, ca.c_str(), nullptr) <= 0) {
        ERR_print_errors_fp(stderr);
        SSL_CTX_free(ctx);
        return 1;
    }

    SSL_CTX_set_verify(ctx, SSL_VERIFY_PEER | SSL_VERIFY_FAIL_IF_NO_PEER_CERT, nullptr);

    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(port);

    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    if (bind(server_fd, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0 || listen(server_fd, 32) < 0) {
        std::cerr << "Failed to bind or listen on port " << port << std::endl;
        close(server_fd);
        SSL_CTX_free(ctx);
        return 1;
    }

    std::cout << "Validator " << validator_id << " listening on " << port << std::endl;

    while (true) {
        sockaddr_in client_addr{};
        socklen_t len = sizeof(client_addr);
        int client_fd = accept(server_fd, reinterpret_cast<sockaddr*>(&client_addr), &len);
        if (client_fd < 0) {
            continue;
        }

        SSL* ssl = SSL_new(ctx);
        SSL_set_fd(ssl, client_fd);
        if (SSL_accept(ssl) <= 0) {
            ERR_print_errors_fp(stderr);
            SSL_free(ssl);
            close(client_fd);
            continue;
        }

        std::thread([ssl, client_fd, validator_id]() {
            handle_client(ssl, validator_id);
            close(client_fd);
        }).detach();
    }

    close(server_fd);
    SSL_CTX_free(ctx);
    return 0;
}
