const colors = {
    website: "#0f7490",
    instagram: "#c13584",
    twitter: "#17202a",
    whatsapp: "#25a55b",
    youtube: "#e62117",
    facebook: "#1877f2",
};

export default function SocialIcon({ network, size = 20 }) {
    const color = colors[network] || colors.website;
    let symbol;

    if (network === "website") {
        symbol = <><circle cx="12" cy="12" r="9" /><path d="M3.5 12h17M12 3c2.3 2.5 3.4 5.5 3.4 9S14.3 18.5 12 21c-2.3-2.5-3.4-5.5-3.4-9S9.7 5.5 12 3Z" /></>;
    } else if (network === "instagram") {
        symbol = <><rect x="3" y="3" width="18" height="18" rx="5" /><circle cx="12" cy="12" r="4" /><circle cx="17.5" cy="6.7" r=".8" fill={color} stroke="none" /></>;
    } else if (network === "twitter") {
        symbol = <path d="M5 4 19 20M19 4 5 20" strokeWidth="2.8" />;
    } else if (network === "whatsapp") {
        symbol = <><path d="M20 11.5a8 8 0 0 1-11.8 7L4 20l1.5-4A8 8 0 1 1 20 11.5Z" /><path d="M9 8.5c.4-.6.8-.5 1.1-.1l1 1.5c.2.3.1.6-.2.9l-.6.6c.6 1.2 1.4 2 2.6 2.6l.6-.7c.3-.3.6-.3.9-.1l1.4.9c.4.3.4.7.1 1.1-.5.7-1.2 1-2 .9-2.8-.5-5.8-3.4-6.2-6.2-.1-.6.4-1.1 1.3-1.4Z" fill={color} stroke="none" /></>;
    } else if (network === "youtube") {
        symbol = <><rect x="2" y="5" width="20" height="14" rx="4" fill={color} stroke="none" /><path d="m10 9 5 3-5 3V9Z" fill="white" stroke="none" /></>;
    } else {
        symbol = <><circle cx="12" cy="12" r="10" fill={color} stroke="none" /><path d="M13.4 21v-8h2.7l.4-3h-3.1V8.1c0-.9.3-1.5 1.6-1.5h1.7V4c-.3 0-1.3-.1-2.5-.1-2.5 0-4.2 1.5-4.2 4.3V10H7.2v3h2.8v8h3.4Z" fill="white" stroke="none" /></>;
    }

    return <svg className={`social-icon social-icon-${network}`} width={size} height={size}
        viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="1.8"
        strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{symbol}</svg>;
}
