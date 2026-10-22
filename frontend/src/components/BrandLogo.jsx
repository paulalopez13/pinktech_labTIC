export default function BrandLogo() {
    return (
        <>
            <span className="brand-mark" aria-hidden="true">
                <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="43" cy="17" r="9" fill="#E8B45D" />
                    <path d="M7 40.5 20.2 24l8.6 10.4 8.5-9 19.7 23.1H7V40.5Z" fill="#78966C" />
                    <path d="M7 43.5c8.8-4.2 16.2-3.2 23.1.2 7.8 3.9 15.7 3.3 26.9-1.2v13H7v-12Z" fill="#176B68" />
                    <path d="M12 49.2c7.4-2.4 13.5-1.7 19.4 1.2 5.1 2.5 10.9 2.6 18.5-.2" stroke="#E7E8C9" strokeWidth="2.2" strokeLinecap="round" />
                    <path d="M20.3 32.2c-.2-5.2 2.8-9 7.7-10.5-.5 4.6-2.8 8-7.7 10.5Z" fill="#A3B87A" />
                    <path d="M18.5 32.4c2.1-1.8 4.5-3.1 7.4-4" stroke="#F0F2E5" strokeWidth="1.3" strokeLinecap="round" />
                </svg>
            </span>
            <span className="brand-wordmark">
                <span>Uruguay</span>
                <strong>Mnatural</strong>
            </span>
        </>
    );
}
