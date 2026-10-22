import { useEffect, useState } from "react";
import { getActivities } from "../services/activityService";

const photos = [
    {
        title: "Casa Pueblo",
        place: "Punta Ballena · Maldonado",
        src: "/images/casa-pueblo.png",
        alt: "Casa Pueblo junto al mar al atardecer",
    },
    {
        title: "Calle de los Suspiros",
        place: "Colonia del Sacramento",
        src: "/images/calle-suspiros.png",
        alt: "Calle empedrada de Colonia del Sacramento con un auto antiguo",
    },
    {
        title: "Bodega de José Ignacio",
        place: "Maldonado",
        src: "/images/bodega-jose-ignacio.png",
        alt: "Viñedos y bodega entre campos de Maldonado",
    },
    {
        title: "Faro de José Ignacio",
        place: "Maldonado",
        src: "/images/faro-jose-ignacio.png",
        alt: "Faro de José Ignacio junto al mar y las rocas",
    },
    {
        title: "Campo de Durazno",
        place: "Durazno",
        src: "/images/campo-durazno.png",
        alt: "Caballos sueltos en un campo abierto de Durazno",
    },
    {
        title: "Cerro Pan de Azúcar",
        place: "Maldonado",
        src: "/images/cerro-pan-azucar.png",
        alt: "Cerro Pan de Azúcar y el paisaje de Maldonado",
    },
    {
        title: "Pozos Azules",
        place: "Maldonado",
        src: "/images/pozos-azules.png",
        alt: "Agua clara entre rocas en Pozos Azules",
    },
    {
        title: "Surf en Cabo Polonio",
        place: "Rocha",
        src: "/images/surf-cabo-polonio.jpg",
        alt: "Olas rompiendo en una playa de Cabo Polonio",
    },
    {
        title: "Médanos de Valizas",
        place: "Rocha",
        src: "/images/medano-valizas.png",
        alt: "Dunas de arena junto al agua en Barra de Valizas",
    },
    {
        title: "Isla Gorriti",
        place: "Punta del Este · Maldonado",
        src: "/images/isla-gorriti.png",
        alt: "Vista aérea de la Isla Gorriti y la bahía de Maldonado",
    },
    {
        title: "Ballena en Punta del Este",
        place: "Maldonado",
        src: "/images/ballena-punta-del-este.png",
        alt: "Ballena franca en el agua frente a Punta del Este",
    },
];

export default function UruguayPhotoCarousel() {
    const [activeIndex, setActiveIndex] = useState(0);
    const [hovered, setHovered] = useState(false);
    const [focused, setFocused] = useState(false);
    const paused = hovered || focused;
    const [activityPhotos, setActivityPhotos] = useState([]);
    const carouselPhotos = activityPhotos.length ? activityPhotos : photos;
    const activePhoto = carouselPhotos[activeIndex % carouselPhotos.length];

    useEffect(() => {
        let active = true;
        getActivities().then(activities => {
            if (!active) return;
            const publishedPhotos = activities.flatMap(activity => (activity.photos || []).map(photo => ({
                title: activity.name,
                place: `${activity.city} · ${activity.department}`,
                src: `http://${window.location.hostname}:8080${photo.url}`,
                alt: `${activity.name}, ${activity.city}, ${activity.department}`,
            })));
            setActivityPhotos(publishedPhotos);
            setActiveIndex(0);
        }).catch(() => {});
        return () => { active = false; };
    }, []);

    useEffect(() => {
        if (paused) return undefined;
        const timer = window.setInterval(() => {
            setActiveIndex(index => (index + 1) % carouselPhotos.length);
        }, 6500);
        return () => window.clearInterval(timer);
    }, [paused, carouselPhotos.length]);

    function showPhoto(index) {
        setActiveIndex((index + carouselPhotos.length) % carouselPhotos.length);
    }

    return (
        <section
            className="home-art photo-carousel"
            aria-label="Paisajes y experiencias de Uruguay"
            aria-roledescription="carrusel"
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            onFocus={() => setFocused(true)}
            onBlur={event => {
                if (!event.currentTarget.contains(event.relatedTarget)) setFocused(false);
            }}
        >
            {carouselPhotos.map((photo, index) => (
                <img
                    key={`${photo.src}-${index}`}
                    className={`carousel-photo${index === activeIndex ? " is-active" : ""}`}
                    src={photo.src}
                    alt={index === activeIndex ? photo.alt : ""}
                    aria-hidden={index !== activeIndex}
                    loading={index < 2 ? "eager" : "lazy"}
                />
            ))}
            <div className="photo-shade" />
            <div className="photo-caption" aria-live="polite" aria-atomic="true">
                <span className="photo-place">{activePhoto.place}</span>
                <h2>{activePhoto.title}</h2>
            </div>
            <div className="carousel-controls" aria-label="Elegir foto">
                <button type="button" className="carousel-arrow" aria-label="Foto anterior" onClick={() => showPhoto(activeIndex - 1)}>‹</button>
                <div className="carousel-dots">
                    {carouselPhotos.map((photo, index) => (
                        <button
                            key={photo.src}
                            type="button"
                            className={`carousel-dot${index === activeIndex ? " is-active" : ""}`}
                            aria-label={`Mostrar ${photo.title}`}
                            aria-current={index === activeIndex ? "true" : undefined}
                            onClick={() => showPhoto(index)}
                        />
                    ))}
                </div>
                <button type="button" className="carousel-arrow" aria-label="Foto siguiente" onClick={() => showPhoto(activeIndex + 1)}>›</button>
            </div>
        </section>
    );
}
