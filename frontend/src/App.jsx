import { useEffect, useState } from "react";
import "./App.css";
import RegistrationTypePage from "./pages/RegistrationTypePage";
import ProviderRegistrationPage from "./pages/ProviderRegistrationPage";
import CompanyProviderRegistrationPage from "./pages/CompanyProviderRegistrationPage";
import IndividualProviderRegistrationPage from "./pages/IndividualProviderRegistrationPage";
import LoginPage from "./pages/LoginPage";
import TouristRegistrationPage from "./pages/TouristRegistrationPage";
import HomePage from "./pages/HomePage";
import ActivitiesPage from "./pages/ActivitiesPage";
import AccountMenuPage from "./pages/AccountMenuPage";
import ProfilePage from "./pages/ProfilePage";
import ProviderApprovalPage from "./pages/ProviderApprovalPage";
import ForgotPasswordPage from "./pages/ForgotPasswordPage";
import ResetPasswordPage from "./pages/ResetPasswordPage";
import { getCurrentUser, logoutUser } from "./services/authService";

function App() {
    const [selectedType, setSelectedType] = useState(() =>
        new URLSearchParams(window.location.search).has("token") ? "resetPassword" : null);
    const [registrationMessage, setRegistrationMessage] = useState("");
    const [loginNotice, setLoginNotice] = useState("");
    const [currentUser, setCurrentUser] = useState(() => {
        try {
            return JSON.parse(sessionStorage.getItem("authenticatedUser")) || null;
        } catch {
            return null;
        }
    });
    const [dashboardNotice, setDashboardNotice] = useState("");

    useEffect(() => {
        let isCurrent = true;
        getCurrentUser()
            .then(user => {
                if (!isCurrent) return;
                sessionStorage.setItem("authenticatedUser", JSON.stringify(user));
                setCurrentUser(user);
            })
            .catch(() => {
                if (!isCurrent) return;
                sessionStorage.removeItem("authenticatedUser");
                setCurrentUser(null);
            });
        return () => { isCurrent = false; };
    }, []);

    function navigate(page, notice = "") {
        if (page !== "resetPassword" && window.location.search.includes("token=")) {
            window.history.replaceState({}, "", window.location.pathname);
        }
        setRegistrationMessage("");
        setLoginNotice(notice);
        setSelectedType(page);
        window.scrollTo(0, 0);
    }

    function handleRegistered(message) {
        setRegistrationMessage(message);
        setSelectedType("login");
        window.scrollTo(0, 0);
    }

    function handleAuthenticated(user) {
        sessionStorage.setItem("authenticatedUser", JSON.stringify(user));
        setCurrentUser(user);
        setRegistrationMessage("");
        setLoginNotice("");
        setDashboardNotice("");
        setSelectedType(null);
        window.scrollTo(0, 0);
    }

    async function handleLogout() {
        try { await logoutUser(); } catch { /* Se limpia la sesión local aunque el servidor no responda. */ }
        sessionStorage.removeItem("authenticatedUser");
        setCurrentUser(null);
        setDashboardNotice("");
        navigate(null);
    }

    let page;
    if (selectedType === "tourist") {
        page = <TouristRegistrationPage onBack={() => navigate("registration")} onRegistered={() => handleRegistered("Tu cuenta de turista se creó correctamente. Ya podés iniciar sesión.")} />;
    } else if (selectedType === "company") {
        page = <CompanyProviderRegistrationPage onBack={() => navigate("provider")} onRegistered={handleRegistered} />;
    } else if (selectedType === "individual") {
        page = <IndividualProviderRegistrationPage onBack={() => navigate("provider")} onRegistered={handleRegistered} />;
    } else if (selectedType === "provider") {
        page = <ProviderRegistrationPage onSelectCompany={() => navigate("company")}
            onSelectIndividual={() => navigate("individual")} onBack={() => navigate("registration")} />;
    } else if (selectedType === "login") {
        page = <LoginPage onRegister={() => navigate("registration")} onForgotPassword={() => navigate("forgotPassword")}
            onAuthenticated={handleAuthenticated}
            registrationMessage={registrationMessage} initialNotice={loginNotice} />;
    } else if (selectedType === "forgotPassword") {
        page = <ForgotPasswordPage onBack={() => navigate("login")} />;
    } else if (selectedType === "resetPassword") {
        page = <ResetPasswordPage token={new URLSearchParams(window.location.search).get("token")}
            onBack={() => navigate("login")} onRequestLink={() => navigate("forgotPassword")} />;
    } else if (selectedType === "profile") {
        page = <ProfilePage onBack={() => navigate(null)} onUpdated={updated => {
            const nextUser = { ...currentUser, username: updated.username, email: updated.email,
                role: updated.role, providerStatus: updated.providerStatus };
            sessionStorage.setItem("authenticatedUser", JSON.stringify(nextUser));
            setCurrentUser(nextUser);
        }} />;
    } else if (selectedType === "approveProviders") {
        page = <ProviderApprovalPage onBack={() => navigate(null)} />;
    } else if (selectedType === "registration") {
        page = <RegistrationTypePage onSelectTourist={() => navigate("tourist")} onSelectProvider={() => navigate("provider")} />;
    } else if (selectedType === "activities") {
        page = <ActivitiesPage onBack={() => navigate(null)} />;
    } else if (currentUser) {
        page = <AccountMenuPage user={currentUser} onExplore={() => navigate("activities")}
            onReservations={() => setDashboardNotice("La opción para consultar tus reservas todavía no está disponible.")}
            onMyActivities={() => setDashboardNotice("La opción para consultar tus actividades todavía no está disponible.")}
            onAddActivity={() => setDashboardNotice("La opción para agregar actividades todavía no está disponible.")}
            onApproveProviders={() => navigate("approveProviders")}
            onEditCategories={() => setDashboardNotice("La edición de categorías todavía no está disponible.")}
            notice={dashboardNotice} />;
    } else {
        page = <HomePage onExplore={() => navigate("activities")} />;
    }

    return (
        <>
            <header className="site-header">
                <button type="button" className="brand-button" onClick={() => navigate(null)}
                    aria-label="Uruguay Mnatural: volver al inicio">Uruguay Mnatural</button>
                <nav className="header-actions" aria-label="Navegación de cuenta">
                    {currentUser ? (
                        <>
                            <button type="button" className="text-button" onClick={() => navigate("profile")}>
                                Mi perfil
                            </button>
                            <button type="button" className="text-button" onClick={handleLogout}>Cerrar sesión</button>
                        </>
                    ) : (
                        <>
                            {selectedType !== "login" && (
                                <button type="button" className="text-button" onClick={() => navigate("registration")}>
                                    Registrarse
                                </button>
                            )}
                            {selectedType !== "login" && (
                                <button type="button" className="text-button" onClick={() => navigate("login")}>
                                    Iniciar sesión
                                </button>
                            )}
                        </>
                    )}
                </nav>
            </header>
            {page}
        </>
    );
}

export default App;
