import UruguayPhotoCarousel from "../components/UruguayPhotoCarousel";

export default function HomePage({ onExplore }) {
    return (
        <main className="registration-page home-page">
            <section className="home-hero">
                <div className="home-copy">
                    <span className="home-eyebrow"><span /> Uruguay natural, a tu ritmo</span>
                    <h1>Hay otro Uruguay esperándote.</h1>
                    <p>Rincones silvestres, aire fresco y experiencias que te acercan a la naturaleza.</p>
                    <button className="home-explore-button" type="button" onClick={onExplore}>
                        Explorar actividades <span aria-hidden="true">↗</span>
                    </button>
                    <div className="home-footnote">
                        <span className="home-footnote-mark" aria-hidden="true">✳</span>
                        <span>Pequeñas escapadas. Grandes recuerdos.</span>
                    </div>
                </div>

                <UruguayPhotoCarousel />
            </section>
        </main>
    );
}
