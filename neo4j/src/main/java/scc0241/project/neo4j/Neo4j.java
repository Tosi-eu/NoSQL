package scc0241.project.neo4j;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

import java.util.Scanner;

public class Neo4j {
    private final Driver driver;

    public Neo4j(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    public void close() {
        driver.close();
    }

    public void addUser(String username) {
        try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork")))  {
            String checkUserQuery = "MATCH (u:User {name: $name}) RETURN COUNT(u) > 0 AS exists";
            boolean userExists = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("name", username))
                                        .single()
                                        .get("exists")
                                        .asBoolean();

            if (!userExists) {
                String query = "CREATE (u:User {name: $name, amigos: 0, comments: 0, posts: 0}) RETURN u";
                session.run(query, org.neo4j.driver.Values.parameters("name", username));
                System.out.println("Usuário adicionado: " + username);
            } else {
                System.out.println("Erro: O usuário " + username + " já existe.");
            }
        }
    }

    public void addFriendship(String user1, String user2) {
        try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork")))  {
            String checkUsersQuery = "MATCH (u1:User {name: $user1}), (u2:User {name: $user2}) RETURN COUNT(u1) > 0 AND COUNT(u2) > 0 AS exists";
            boolean usersExist = session.run(checkUsersQuery, org.neo4j.driver.Values.parameters("user1", user1, "user2", user2))
                                         .single()
                                         .get("exists") 
                                         .asBoolean();

            if (usersExist) {
                String query = "MATCH (u1:User {name: $user1}), (u2:User {name: $user2}) " +
                               "CREATE (u1)-[:FRIEND]->(u2) " +
                               "SET u1.amigos = u1.amigos + 1, u2.amigos = u2.amigos + 1";
                session.run(query, org.neo4j.driver.Values.parameters("user1", user1, "user2", user2));
                System.out.println("Amizade adicionada entre " + user1 + " e " + user2);
            } else {
                System.out.println("Erro: Um ou ambos os usuários não existem.");
            }
        }
    }

    public void deleteFriendship(String user1, String user2) {
        try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork")))  {
            String query = "MATCH (u1:User {name: $user1})-[r:FRIEND]->(u2:User {name: $user2}) " +
                           "DELETE r " +
                           "SET u1.amigos = u1.amigos - 1, u2.amigos = u2.amigos - 1";
            session.run(query, org.neo4j.driver.Values.parameters("user1", user1, "user2", user2));
            System.out.println("Amizade removida entre " + user1 + " e " + user2);
        }
    }

    public void addPost(String username, String content) {
        try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork")))  {
            String checkUserQuery = "MATCH (u:User {name: $username}) RETURN COUNT(u) > 0 AS exists";
            boolean userExists = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("username", username))
                                        .single()
                                        .get("exists")
                                        .asBoolean();

            if (userExists) {
                String query = "MATCH (u:User {name: $username}) " +
                               "CREATE (u)-[:POSTED]->(p:Post {content: $content}) RETURN p" +
                               "SET u.posts = u.posts + 1 RETURN p";
                session.run(query, org.neo4j.driver.Values.parameters("username", username, "content", content));
                System.out.println("Post adicionado por " + username + ": " + content);
            } else {
                System.out.println("Erro: O usuário não existe.");
            }
        }
    }

public void removePost(String username, String content) {
    try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork"))) {

        String checkUserQuery = "MATCH (u:User {name: $username}) RETURN COUNT(u) > 0 AS userExists";
        boolean userExists = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("username", username))
                                    .single()
                                    .get("userExists")
                                    .asBoolean();

        String checkPostQuery = "MATCH (u:User {name: $username})-[r:POSTED]->(p:Post {content: $content}) RETURN COUNT(p) > 0 AS postExists";
        boolean postExists = session.run(checkPostQuery, org.neo4j.driver.Values.parameters("username", username, "content", content))
                                    .single()
                                    .get("postExists")
                                    .asBoolean();

        if (userExists && postExists) {
            try (var tx = session.beginTransaction()) {
                
                String deleteCommentsQuery = 
                    "MATCH (u:User {name: $username})-[:POSTED]->(p:Post {content: $content})<-[:ON_POST]-(c:Comment) " +
                    "DETACH DELETE c";
                tx.run(deleteCommentsQuery, org.neo4j.driver.Values.parameters("username", username, "content", content));

                String deletePostQuery = 
                    "MATCH (u:User {name: $username})-[r:POSTED]->(p:Post {content: $content}) " +
                    "DETACH DELETE p " +
                    "SET u.posts = u.posts - 1";
                tx.run(deletePostQuery, org.neo4j.driver.Values.parameters("username", username, "content", content));

                tx.commit();
                System.out.println("Post e seus comentários removidos de " + username + ": " + content);
            } catch (Exception e) {
                throw e;
            }
        } else {
            if (!userExists) {
                System.out.println("Erro: O usuário " + username + " não existe.");
            }
            if (!postExists) {
                System.out.println("Erro: O post com conteúdo \"" + content + "\" não existe.");
            }
        }
    }
}

  public void editPost(String username, String oldContent, String newContent) {
    try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork"))) {
        
        String checkUserQuery = "MATCH (u:User {name: $username}) RETURN COUNT(u) > 0 AS exists";
        boolean userExists = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("username", username))
                                    .single()
                                    .get("exists")
                                    .asBoolean();
        
        if (!userExists) {
            System.out.println("Erro: O usuário " + username + " não existe.");
            return;
        }

        String checkContentQuery = "MATCH (u:User {name: $username})-[r:POSTED]->(p:Post {content: $oldContent}) RETURN COUNT(p) > 0 AS existsContent";
        boolean contentExists = session.run(checkContentQuery, org.neo4j.driver.Values.parameters("username", username, "oldContent", oldContent))
                                    .single()
                                    .get("existsContent")
                                    .asBoolean();

        if (!contentExists) {
            System.out.println("Erro: O post " + oldContent + " não existe.");
            return;
        }
        
        String updateQuery = "MATCH (u:User {name: $username})-[r:POSTED]->(p:Post {content: $oldContent}) " +
                             "SET p.content = $newContent";
        session.run(updateQuery, org.neo4j.driver.Values.parameters("username", username, "oldContent", oldContent, "newContent", newContent));
        System.out.println("Post editado de " + username + ": " + oldContent + " -> " + newContent);
    }
}

    
public void editComment(String username, String oldContent, String newContent) {
    try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork"))) {

        String checkUserQuery = "MATCH (u:User {name: $username}) RETURN COUNT(u) > 0 AS exists";
        boolean userExists = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("username", username))
                                    .single()
                                    .get("exists")
                                    .asBoolean();

        if (!userExists) {
            System.out.println("Erro: O usuário " + username + " não existe.");
            return;
        }

        String checkCommentQuery = "MATCH (u:User {name: $username})-[r:COMMENTED]->(c:Comment {content: $oldContent}) RETURN COUNT(c) > 0 AS exists";
        boolean commentExists = session.run(checkCommentQuery, org.neo4j.driver.Values.parameters("username", username, "oldContent", oldContent))
                                       .single()
                                       .get("exists")
                                       .asBoolean();

        if (!commentExists) {
            System.out.println("Erro: O comentário \"" + oldContent + "\" não existe.");
            return;
        }

        String query = "MATCH (u:User {name: $username})-[r:COMMENTED]->(c:Comment {content: $oldContent}) " +
                       "SET c.content = $newContent";
        session.run(query, org.neo4j.driver.Values.parameters("username", username, "oldContent", oldContent, "newContent", newContent));
        System.out.println("Comentário editado de " + username + ": " + oldContent + " -> " + newContent);
    } catch (Exception e) {
        System.out.println("Erro ao editar o comentário: " + e.getMessage());
    }
}

    public void adicionarComentario(String postOwner, String commenter, String postContent, String commentContent) {
        try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork")))  {
            String checkUsersQuery = "MATCH (postOwner:User {name: $postOwner}), (commenter:User {name: $commenter}) " +
                                     "RETURN COUNT(postOwner) > 0 AND COUNT(commenter) > 0 AS usersExist";
            boolean usersExist = session.run(checkUsersQuery, org.neo4j.driver.Values.parameters("postOwner", postOwner, "commenter", commenter))
                                         .single()
                                         .get("usersExist")
                                         .asBoolean();

            if (usersExist) {
                String checkPostQuery = "MATCH (postOwner:User {name: $postOwner})-[:POSTED]->(p:Post {content: $postContent}) RETURN COUNT(p) > 0 AS postExists";
                boolean postExists = session.run(checkPostQuery, org.neo4j.driver.Values.parameters("postOwner", postOwner, "postContent", postContent))
                                             .single()
                                             .get("postExists")
                                             .asBoolean();

                if (postExists) {
                    String addCommentQuery = "MATCH (commenter:User {name: $commenter}), (p:Post {content: $postContent}) " +
                                             "CREATE (commenter)-[:COMMENTED]->(c:Comment {content: $commentContent}), " +
                                             "(c)-[:ON_POST]->(p)" +
                                             "MATCH (commenter) SET commenter.comments = commenter.comments + 1";
                    session.run(addCommentQuery, org.neo4j.driver.Values.parameters("commenter", commenter, "postContent", postContent, "commentContent", commentContent));
                    System.out.println("Comentário adicionado por " + commenter + " no post de " + postOwner);
                } else {
                    System.out.println("Erro: O post com o conteúdo \"" + postContent + "\" não existe.");
                }
            } else {
                System.out.println("Erro: Um ou ambos os usuários não existem.");
            }
        }
    }

 public void deleteUser(String username) {
    try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork"))) {
        String checkUserQuery = "MATCH (u:User {name: $username}) RETURN u";
        var userResult = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("username", username));

        if (userResult.hasNext()) {
            try (var tx = session.beginTransaction()) {
                String deleteUserCommentsQuery = 
                    "MATCH (u:User {name: $username})-[r:COMMENTED]->(c:Comment) " +
                    "DETACH DELETE c " +
                    "MATCH (u) SET u.comments = u.comments - 1";

                tx.run(deleteUserCommentsQuery, org.neo4j.driver.Values.parameters("username", username));

                String deleteCommentsOnUserPostsQuery = 
                    "MATCH (u:User {name: $username})-[:POSTED]->(p:Post)<-[:ON_POST]-(c:Comment) " +
                    "DETACH DELETE c";

                tx.run(deleteCommentsOnUserPostsQuery, org.neo4j.driver.Values.parameters("username", username));

                String deleteUserPostsQuery = 
                    "MATCH (u:User {name: $username})-[:POSTED]->(p:Post) " +
                    "DETACH DELETE p";

                tx.run(deleteUserPostsQuery, org.neo4j.driver.Values.parameters("username", username));

                String detachUserQuery = 
                    "MATCH (u:User {name: $username}) " +
                    "DETACH DELETE u";

                tx.run(detachUserQuery, org.neo4j.driver.Values.parameters("username", username));
                tx.commit();

                System.out.println("Usuario e todos os relacionamentos e nodes associados foram removidos: " + username);
            }
        } else {
            System.out.println("Erro: O usuário nao existe.");
        }
    }
}

   public static void main(String... args) {
        final String dbUri = "neo4j://localhost:7687";
        final String dbUser = "neo4j";
        final String dbPassword = "neo4j_Eu12";

        try {
            Neo4j socialNetwork = new Neo4j(dbUri, dbUser, dbPassword);
            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                System.out.println("\nMenu:");
                System.out.println("1. Criar Usuario");
                System.out.println("2. Adicionar Amizade");
                System.out.println("3. Remover Amizade");
                System.out.println("4. Adicionar Post");
                System.out.println("5. Remover Post");
                System.out.println("6. Editar Post");
                System.out.println("7. Adicionar Comentario");
                System.out.println("8. Editar Comentario");
                System.out.println("9. Remover Usuario");
                System.out.println("10. Sair");
                System.out.print("Escolha uma opcao: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); 

                switch (choice) {
                    case 1 -> {
                        System.out.print("Digite o nome do usuario: ");
                        String username = scanner.nextLine();
                        socialNetwork.addUser(username);
                    }
                    case 2 -> {
                        System.out.print("Digite o nome do primeiro usuário: ");
                        String user1 = scanner.nextLine();
                        System.out.print("Digite o nome do segundo usuário: ");
                        String user2 = scanner.nextLine();
                        socialNetwork.addFriendship(user1, user2);
                    }
                    case 3 -> {
                        System.out.print("Digite o nome do primeiro usuário: ");
                        String user1ToRemove = scanner.nextLine();
                        System.out.print("Digite o nome do segundo usuário: ");
                        String user2ToRemove = scanner.nextLine();
                        socialNetwork.deleteFriendship(user1ToRemove, user2ToRemove);
                    }
                    case 4 -> {
                        System.out.print("Digite o nome do usuário: ");
                        String postUser = scanner.nextLine();
                        System.out.print("Digite o conteúdo do post: ");
                        String postContent = scanner.nextLine();
                        socialNetwork.addPost(postUser, postContent);
                    }
                    case 5 -> {
                        System.out.print("Digite o nome do usuário: ");
                        String removePostUser = scanner.nextLine();
                        System.out.print("Digite o conteúdo do post a ser removido: ");
                        String removePostContent = scanner.nextLine();
                        socialNetwork.removePost(removePostUser, removePostContent);
                    }
                    case 6 -> {
                        System.out.print("Digite o nome do usuário: ");
                        String editPostUser = scanner.nextLine();
                        System.out.print("Digite o conteúdo antigo do post: ");
                        String oldPostContent = scanner.nextLine();
                        System.out.print("Digite o novo conteúdo do post: ");
                        String newPostContent = scanner.nextLine();
                        socialNetwork.editPost(editPostUser, oldPostContent, newPostContent);
                    }
                    case 7 -> {
                        System.out.print("Digite o nome do dono do post: ");
                        String postOwner = scanner.nextLine();
                        System.out.print("Digite o nome do usuário que está comentando: ");
                        String commenter = scanner.nextLine();
                        System.out.print("Digite o conteúdo do post: ");
                        String postContent = scanner.nextLine();
                        System.out.print("Digite o conteúdo do comentário: ");
                        String commentContent = scanner.nextLine();
                        socialNetwork.adicionarComentario(postOwner, commenter, postContent, commentContent);
                    }
                    case 8 -> {
                        System.out.print("Digite o nome do usuário que comentou: ");
                        String userToEditComment = scanner.nextLine();
                        System.out.print("Comentario anterior: ");
                        String oldCommentContent = scanner.nextLine();
                        System.out.print("Novo comentario: ");
                        String newCommentContent = scanner.nextLine();
                        socialNetwork.editComment(userToEditComment, oldCommentContent, newCommentContent);
                    }
                    case 9 -> {
                    System.out.print("Digite o nome do usuário a ser removido: ");
                    String userToDelete = scanner.nextLine();
                    socialNetwork.deleteUser(userToDelete);
                    }
                    case 10 -> running = false;
                    default -> System.out.println("Opção inválida. Tente novamente.");
                }
                
            }

            socialNetwork.close();
        } catch (ServiceUnavailableException e) {
            System.out.println("Erro ao conectar-se ao Neo4j: " + e.getMessage());
        }
    }
}
